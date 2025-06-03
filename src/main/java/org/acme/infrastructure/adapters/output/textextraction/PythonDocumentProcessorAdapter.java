package org.acme.infrastructure.adapters.output.textextraction;

import org.acme.application.ports.output.textextraction.DocumentProcessorPort;
import org.acme.domain.textextraction.exception.TextExtractionException;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Base64;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.acme.infrastructure.providers.qualifers.DocumentProcessorQualifiers.PythonTesseract;

@ApplicationScoped
@PythonTesseract
public class PythonDocumentProcessorAdapter implements DocumentProcessorPort {

    private static final Logger logger = Logger.getLogger(PythonDocumentProcessorAdapter.class);

    @ConfigProperty(name = "python.service.url")
    String pythonServiceUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PythonDocumentProcessorAdapter() {
        logger.info("PythonDocumentProcessorAdapter inicializado");
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String extractText(Path filePath, String fileType, String language) {
        logger.debug("Iniciando extracción de texto con servicio Python para archivo: " + filePath);

        try {
            byte[] fileContent = Files.readAllBytes(filePath);
            String base64Content = Base64.getEncoder().encodeToString(fileContent);
            String fileName = filePath.getFileName().toString();

            Map<String, Object> sourceMap = new HashMap<>();
            sourceMap.put("data", base64Content);
            sourceMap.put("media_type", "application/" + fileType.toLowerCase());
            sourceMap.put("type", "base64");
            sourceMap.put("status", "pending");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("file_name", fileName);
            requestBody.put("file_type", fileType.toLowerCase());
            requestBody.put("source", sourceMap);

            String jsonRequest = objectMapper.writeValueAsString(requestBody);
            logger.debug("Request JSON being sent: " + jsonRequest);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(pythonServiceUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .timeout(Duration.ofMinutes(2))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                String errorDetails = "Status: " + response.statusCode() +
                        "\nHeaders: " + response.headers() +
                        "\nBody: " + response.body();
                logger.error("Error del servicio Python:\n" + errorDetails);
                throw new TextExtractionException("Error en el servicio Python:\n" + errorDetails);
            }

            Map<String, Object> pythonResponse = objectMapper.readValue(response.body(), Map.class);
            Map<String, Object> responseSource = (Map<String, Object>) pythonResponse.get("source");

            String extractedText = (String) responseSource.get("data");

            if (extractedText == null || extractedText.isEmpty()) {
                logger.warn("El servicio Python no devolvió texto para el archivo: " + fileName);
                return "";
            }

            logger.info("Texto extraído exitosamente con servicio Python para: " + fileName);
            return extractedText;

        } catch (Exception e) {
            logger.error("Error inesperado al procesar documento con servicio Python", e);
            throw new TextExtractionException("Error inesperado: " + e.getMessage(), e);
        }
    }
}
