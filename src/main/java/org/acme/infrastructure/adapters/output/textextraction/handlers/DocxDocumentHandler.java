package org.acme.infrastructure.adapters.output.textextraction.handlers;

import org.acme.domain.textextraction.exception.TextExtractionException;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.FileInputStream;
import java.nio.file.Path;

@ApplicationScoped
public class DocxDocumentHandler {
    
    private static final Logger logger = Logger.getLogger(DocxDocumentHandler.class);
    
    public String extractText(Path filePath, String language) {
        logger.debug("Extrayendo texto de archivo DOCX: " + filePath);
        
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            
            String text = extractor.getText();
            logger.info("Texto extraído exitosamente del archivo DOCX");
            return text;
            
        } catch (Exception e) {
            logger.error("Error al extraer texto del archivo DOCX", e);
            throw new TextExtractionException("Error al extraer texto del archivo DOCX: " + e.getMessage(), e);
        }
    }
}