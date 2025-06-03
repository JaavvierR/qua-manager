package org.acme.infrastructure.adapters.output.textextraction;

import org.acme.infrastructure.adapters.output.textextraction.handlers.*;
import org.acme.domain.textextraction.exception.TextExtractionException;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.file.Path;

@ApplicationScoped
public class DocumentHandlerFactory {
    
    private static final Logger logger = Logger.getLogger(DocumentHandlerFactory.class);
    
    private final DocxDocumentHandler docxHandler;
    private final PdfDocumentHandler pdfHandler;
    private final ImageDocumentHandler imageHandler;
    private final JsonDocumentHandler jsonHandler;
    private final YamlDocumentHandler yamlHandler;
    
    @Inject
    public DocumentHandlerFactory(
            DocxDocumentHandler docxHandler,
            PdfDocumentHandler pdfHandler,
            ImageDocumentHandler imageHandler,
            JsonDocumentHandler jsonHandler,
            YamlDocumentHandler yamlHandler) {
        this.docxHandler = docxHandler;
        this.pdfHandler = pdfHandler;
        this.imageHandler = imageHandler;
        this.jsonHandler = jsonHandler;
        this.yamlHandler = yamlHandler;
        logger.info("DocumentHandlerFactory inicializado con todos los handlers");
    }
    
    public String processDocument(Path filePath, String fileType, String language) {
        logger.debug("Procesando documento tipo: " + fileType);
        
        switch (fileType.toLowerCase()) {
            case "pdf":
                logger.debug("Usando PdfDocumentHandler");
                return pdfHandler.extractText(filePath, language);
            case "docx":
                logger.debug("Usando DocxDocumentHandler");
                return docxHandler.extractText(filePath, language);
            case "jpg":
            case "jpeg":
            case "png":
                logger.debug("Usando ImageDocumentHandler");
                return imageHandler.extractText(filePath, language);
            case "json":
                logger.debug("Usando JsonDocumentHandler");
                return jsonHandler.extractText(filePath, language);
            case "yaml":
            case "yml":
                logger.debug("Usando YamlDocumentHandler");
                return yamlHandler.extractText(filePath, language);
            default:
                String message = "Tipo de archivo no soportado: " + fileType;
                logger.error(message);
                throw new TextExtractionException(message);
        }
    }
}