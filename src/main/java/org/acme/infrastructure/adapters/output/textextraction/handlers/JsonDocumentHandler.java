package org.acme.infrastructure.adapters.output.textextraction.handlers;

import org.acme.domain.textextraction.exception.TextExtractionException;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
public class JsonDocumentHandler {
    
    private static final Logger logger = Logger.getLogger(JsonDocumentHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public String extractText(Path filePath, String language) {
        logger.debug("Procesando archivo JSON: " + filePath);
        
        try {
            // Leer el archivo JSON completo
            String jsonContent = Files.readString(filePath);
            
            // Parsear el JSON para validar que es correcto
            JsonNode jsonNode = objectMapper.readTree(jsonContent);
            
            // Convertir el JSON a un string formateado
            String formattedJson = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(jsonNode);
            
            logger.info("Archivo JSON procesado exitosamente");
            return formattedJson;
            
        } catch (Exception e) {
            logger.error("Error al procesar archivo JSON", e);
            throw new TextExtractionException("Error al procesar archivo JSON: " + e.getMessage(), e);
        }
    }
}