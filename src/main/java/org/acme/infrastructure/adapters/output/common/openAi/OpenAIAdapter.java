package org.acme.infrastructure.adapters.output.common.openAi;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.acme.application.ports.output.common.TextCompletionPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jboss.logging.Logger;

import org.acme.infrastructure.providers.qualifers.AIProviderQualifiers.OpenAI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.core.buffer.Buffer; 
import io.vertx.mutiny.core.Vertx;
import org.acme.domain.textcomplection.TextProcessingException;

import io.smallrye.mutiny.Uni;

@OpenAI
@ApplicationScoped
public class OpenAIAdapter implements TextCompletionPort {
    private static final Logger LOG = Logger.getLogger(OpenAIAdapter.class); // Cambiado para usar la clase correcta
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ThreadLocal<String> currentExtractedText = new ThreadLocal<>();
    
    private final OpenAiConfig config;
    
    // Variables para control de tiempo
    private long startTime = 0;
    private long pauseTime = 0;
    private boolean timerRunning = false;
    private double elapsedTime = 0;
    
    // WebClient como campo de la clase
    private WebClient webClient;
    
    // Inyectar Vertx para crear el WebClient
    @Inject
    Vertx vertx;
    
    @Inject
    public OpenAIAdapter(@OpenAI OpenAiConfig config) {
        this.config = config;
    }
    
    @PostConstruct
    void init() {
        LOG.info("⚡ Cargando API Key: " + (config.getApiKey() != null && !config.getApiKey().isEmpty() ? "OK" : "FALLO"));
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            throw new RuntimeException("⚠️ API Key de OpenAI no configurada o vacía");
        }
        // Crear el WebClient después de que Vertx esté disponible
        this.webClient = WebClient.create(vertx);
    }
    
    
    @Override
    public String getModelName() {
        return config.getModelName();
    }
    
    @Override
    public void startTimer() {
        if (!timerRunning) {
            startTime = System.currentTimeMillis();
            timerRunning = true;
            LOG.info("Temporizador iniciado");
        }
    }
    
    @Override
    public void pauseTimer() {
        if (timerRunning) {
            pauseTime = System.currentTimeMillis();
            timerRunning = false;
            elapsedTime += (pauseTime - startTime) / 1000.0;
            LOG.info("Temporizador pausado. Tiempo transcurrido: " + elapsedTime + " segundos");
        }
    }
    
    @Override
    public double getElapsedTime() {
        if (timerRunning) {
            long currentTime = System.currentTimeMillis();
            return elapsedTime + (currentTime - startTime) / 1000.0;
        } else {
            return elapsedTime;
        }
    }


    @Override
    public Uni<String> generateResponseAsync(String validationPrompt) {
        LOG.info("Generando respuesta asíncrona para prompt de validación: " + validationPrompt);
        
        try {
            // Preparar los datos de la solicitud
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("model", config.getModelName());
            requestData.put("temperature", config.getDefaultTemperature());
            
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", config.getSystemPrompt()));
            messages.add(Map.of("role", "user", "content", validationPrompt));
            requestData.put("messages", messages);
            
            String jsonRequest = MAPPER.writeValueAsString(requestData);
            LOG.info("Solicitud JSON para validación asíncrona: " + jsonRequest);
            
            // Realizar la llamada HTTP asíncrona
            return webClient
                .postAbs(config.getApiUrl())
                .putHeader("Content-Type", "application/json")
                .putHeader("Authorization", "Bearer " + config.getApiKey())
                .sendBuffer(Buffer.buffer(jsonRequest))
                .onItem().transform(response -> {
                    int statusCode = response.statusCode();
                    String responseBody = response.bodyAsString();
                    
                    LOG.info("Código de respuesta asíncrona: " + statusCode);
                    LOG.info("Respuesta completa de validación asíncrona: " + responseBody);
                    
                    if (statusCode >= 200 && statusCode < 300) {
                        try {
                            JsonNode jsonResponse = MAPPER.readTree(responseBody);
                            String content = jsonResponse.path("choices").get(0).path("message").path("content").asText();
                            LOG.info("Respuesta final de validación asíncrona: " + content);
                            return content;
                        } catch (Exception e) {
                            LOG.error("Error al parsear respuesta JSON: " + e.getMessage());
                            throw new TextProcessingException("Error al parsear respuesta JSON: " + e.getMessage(), e);
                        }
                    } else {
                        throw new TextProcessingException("Error en la respuesta de validación de OpenAI: " + responseBody);
                    }
                })
                .onFailure().transform(throwable -> {
                    LOG.error("Error al procesar la solicitud de validación asíncrona: " + throwable.getMessage());
                    return new TextProcessingException("Error al procesar la solicitud de validación asíncrona: " + throwable.getMessage(), throwable);
                });
                
        } catch (Exception e) {
            LOG.error("Error al preparar la solicitud de validación asíncrona: " + e.getMessage());
            return Uni.createFrom().failure(new TextProcessingException("Error al preparar la solicitud de validación asíncrona: " + e.getMessage(), e));
        }
    }

    

}