package org.acme.infrastructure.adapters.output.textextraction;

import org.acme.application.ports.output.textextraction.DocumentProcessorPort;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.file.Path;

import org.acme.infrastructure.providers.qualifers.DocumentProcessorQualifiers.JavaTesseract;

@ApplicationScoped
@JavaTesseract
public class DocumentProcessorAdapter implements DocumentProcessorPort {
    
    private static final Logger logger = Logger.getLogger(DocumentProcessorAdapter.class);
    
    private final DocumentHandlerFactory handlerFactory;
    
    @Inject
    public DocumentProcessorAdapter(DocumentHandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
        logger.info("DocumentProcessorAdapter (Tesseract) inicializado");
    }
    
    @Override
    public String extractText(Path filePath, String fileType, String language) {
        logger.debug("Iniciando extracción de texto con Tesseract para archivo: " + filePath);
        return handlerFactory.processDocument(filePath, fileType, language);
    }
}