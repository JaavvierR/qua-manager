package org.acme.infrastructure.adapters.output.textextraction.handlers;

import org.acme.domain.textextraction.exception.TextExtractionException;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@ApplicationScoped
public class YamlDocumentHandler {
    
    private static final Logger logger = Logger.getLogger(YamlDocumentHandler.class);
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    public String extractText(Path filePath, String language) {
        logger.debug("Procesando archivo YAML: " + filePath);
        
        try {
            // Leer el archivo YAML completo
            String yamlContent = Files.readString(filePath);
            
            // Convertir el YAML a un objeto Map para validar que es correcto
            Map<String, Object> yamlMap = yamlMapper.readValue(yamlContent, new TypeReference<Map<String, Object>>() {});
            
            // Volver a convertir a YAML formateado para mantener el formato
            String formattedYaml = yamlMapper.writeValueAsString(yamlMap);
            
            logger.info("Archivo YAML procesado exitosamente");
            return formattedYaml;
            
        } catch (Exception e) {
            logger.error("Error al procesar archivo YAML", e);
            throw new TextExtractionException("Error al procesar archivo YAML: " + e.getMessage(), e);
        }
    }
}