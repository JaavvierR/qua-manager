package org.acme.infrastructure.adapters.output.textextraction.utils;

import org.acme.domain.textextraction.exception.TextExtractionException;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class Base64Decoder {
    
    private static final Logger logger = Logger.getLogger(Base64Decoder.class);
    
    public static Path decodeToTempFile(String base64Content, String fileExtension) {
        logger.debug("Decodificando contenido base64 a archivo temporal");
        
        try {
            // Decodificar el contenido base64
            byte[] decodedBytes = Base64.getDecoder().decode(base64Content);
            
            // Crear archivo temporal
            Path tempFile = Files.createTempFile("document-", "." + fileExtension);
            Files.write(tempFile, decodedBytes);
            
            return tempFile;
            
        } catch (IllegalArgumentException e) {
            logger.error("Error al decodificar el contenido base64", e);
            throw new TextExtractionException("El contenido base64 no es válido", e);
        } catch (IOException e) {
            logger.error("Error al escribir el archivo temporal", e);
            throw new TextExtractionException("Error al crear el archivo temporal: " + e.getMessage(), e);
        }
    }
}