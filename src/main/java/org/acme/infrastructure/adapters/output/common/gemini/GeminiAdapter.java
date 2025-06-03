package org.acme.infrastructure.adapters.output.common.gemini;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.acme.application.ports.output.common.TextCompletionPort;
import org.acme.infrastructure.providers.qualifers.AIProviderQualifiers.Gemini;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

@ApplicationScoped
@Gemini
public class GeminiAdapter implements TextCompletionPort {
    private static final Logger LOG = Logger.getLogger(GeminiAdapter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ThreadLocal<String> currentExtractedText = new ThreadLocal<>();
    
    private final GeminiConfig config;
    
    private long startTime = 0;
    private long pauseTime = 0;
    private boolean timerRunning = false;
    private double elapsedTime = 0;
    
    @Inject
    public GeminiAdapter(@Gemini GeminiConfig config) {
        this.config = config;
    }
    
    @PostConstruct
    void init() {
        LOG.info("⚡ Cargando API Key de Gemini: " + (config.getApiKey() != null && !config.getApiKey().isEmpty() ? "OK" : "FALLO"));
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            throw new RuntimeException("⚠️ API Key de Gemini no configurada o vacía");
        }
    }
    
    @Override
    public Uni<String> generateResponseAsync(String validationPrompt) {
        LOG.info("Generando respuesta reactiva para prompt de validación");
        
        return Uni.createFrom().item(() -> {
                // Llamar al método de envío de solicitud
                return sendRequestToGemini(validationPrompt, config.getDefaultTemperature());
            })
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
            .onFailure().transform(throwable -> {
                LOG.error("Error al procesar la solicitud de validación reactiva: " + throwable.getMessage(), throwable);
                return new RuntimeException("Error al procesar la solicitud de validación: " + throwable.getMessage(), throwable);
            });
    }
    
    /**
     * Envía la solicitud HTTP a la API de Gemini
     */
    private String sendRequestToGemini(String prompt, double temperature) {
        try {
            // Construir la URL de la API
            String apiUrl = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", 
                                        config.getModelName(), config.getApiKey());
            
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Configurar la conexión
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            
            // Construir el cuerpo de la solicitud
            ObjectNode requestBody = MAPPER.createObjectNode();
            ArrayNode contents = MAPPER.createArrayNode();
            ObjectNode content = MAPPER.createObjectNode();
            ArrayNode parts = MAPPER.createArrayNode();
            ObjectNode part = MAPPER.createObjectNode();
            part.put("text", prompt);
            parts.add(part);
            content.set("parts", parts);
            contents.add(content);
            requestBody.set("contents", contents);
            
            // Configurar parámetros de generación
            ObjectNode generationConfig = MAPPER.createObjectNode();
            generationConfig.put("temperature", temperature);
            requestBody.set("generationConfig", generationConfig);
            
            // Enviar la solicitud
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = MAPPER.writeValueAsString(requestBody).getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Leer la respuesta
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    // Parsear la respuesta JSON
                    JsonNode jsonResponse = MAPPER.readTree(response.toString());
                    return extractTextFromResponse(jsonResponse);
                }
            } else {
                // Leer el error
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        errorResponse.append(responseLine.trim());
                    }
                    throw new RuntimeException("Error en la API de Gemini: " + responseCode + " - " + errorResponse);
                }
            }
            
        } catch (Exception e) {
            LOG.error("Error al enviar solicitud a Gemini: " + e.getMessage(), e);
            throw new RuntimeException("Error al comunicarse con la API de Gemini", e);
        }
    }
    
    /**
     * Extrae el texto de la respuesta de Gemini
     */
    private String extractTextFromResponse(JsonNode response) {
        try {
            JsonNode candidates = response.get("candidates");
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        JsonNode firstPart = parts.get(0);
                        JsonNode text = firstPart.get("text");
                        if (text != null) {
                            return text.asText();
                        }
                    }
                }
            }
            throw new RuntimeException("No se pudo extraer el texto de la respuesta de Gemini");
        } catch (Exception e) {
            LOG.error("Error al parsear respuesta de Gemini: " + e.getMessage(), e);
            throw new RuntimeException("Error al parsear la respuesta de Gemini", e);
        }
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
        }
    }
    
    @Override
    public void pauseTimer() {
        if (timerRunning) {
            pauseTime = System.currentTimeMillis();
            timerRunning = false;
            elapsedTime += (pauseTime - startTime) / 1000.0;
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
}