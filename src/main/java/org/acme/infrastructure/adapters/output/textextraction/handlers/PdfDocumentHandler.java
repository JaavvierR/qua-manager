package org.acme.infrastructure.adapters.output.textextraction.handlers;

import org.acme.domain.textextraction.exception.TextExtractionException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jboss.logging.Logger;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

@Singleton
public class PdfDocumentHandler {
    
    private static final Logger logger = Logger.getLogger(PdfDocumentHandler.class);
    
    private final ImageDocumentHandler imageDocumentHandler;
    
    @ConfigProperty(name = "pdf.text.extraction.ocr.enabled")
    boolean ocrEnabledByDefault;

    @Inject
    public PdfDocumentHandler(ImageDocumentHandler imageDocumentHandler) {
        this.imageDocumentHandler = imageDocumentHandler;
        logger.infof("Valor de OCR_ENABLE: %s", System.getenv("OCR_ENABLE"));
        logger.infof("PdfDocumentHandler inicializado. OCR habilitado por defecto: %b", ocrEnabledByDefault);
    }
    
    /**
     * Extrae texto del PDF usando configuración por defecto (de properties)
     */
    public String extractText(Path filePath, String language) {
        return extractText(filePath, language, ocrEnabledByDefault);
    }
    
    /**
     * Versión sobrecargada que permite sobrescribir la configuración
     */
    public String extractText(Path filePath, String language, boolean useOcr) {
        logger.debugf("Extrayendo texto de PDF: %s. Usando OCR: %b", filePath, useOcr);
        
        try (PDDocument document = PDDocument.load(filePath.toFile())) {
            if (useOcr) {
                try {
                    String ocrText = extractTextWithOcr(document, language);
                    if (isTextSubstantial(ocrText, document.getNumberOfPages())) {
                        logger.info("Texto extraído exitosamente con OCR");
                        return ocrText;
                    }
                    logger.info("OCR produjo texto insuficiente, intentando extracción nativa");
                } catch (Exception e) {
                    logger.warn("Falló el OCR, recurriendo a extracción nativa", e);
                }
            }
            
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
            
        } catch (Exception e) {
            logger.error("Error al extraer texto del PDF", e);
            throw new TextExtractionException("Error al extraer texto del PDF: " + e.getMessage(), e);
        }
    }
    
    /**
     * Determina si el texto extraído parece sustancial basado en la cantidad de páginas
     */
    private boolean isTextSubstantial(String text, int pageCount) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // Estimación simple: aproximadamente 200 caracteres por página como mínimo
        int minExpectedChars = pageCount * 200;
        return text.length() >= minExpectedChars;
    }
    
    /**
     * Extrae texto del PDF convirtiéndolo a imágenes y aplicando OCR
     */
    private String extractTextWithOcr(PDDocument document, String language) throws Exception {
        logger.debug("Convirtiendo PDF a imágenes para OCR");
        
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        StringBuilder fullText = new StringBuilder();
        Path tempDir = Files.createTempDirectory("pdf_images_");
        
        try {
            int pageCount = document.getNumberOfPages();
            List<Path> imagePaths = new ArrayList<>();
            
            // Renderizar cada página como imagen
            for (int i = 0; i < pageCount; i++) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(i, 300); // DPI más alto para mejor OCR
                Path imagePath = tempDir.resolve("page_" + (i + 1) + ".png");
                ImageIO.write(image, "PNG", imagePath.toFile());
                imagePaths.add(imagePath);
                
                logger.debug("Página " + (i + 1) + " convertida a imagen: " + imagePath);
            }
            
            // Aplicar OCR a cada imagen usando ImageDocumentHandler
            for (int i = 0; i < imagePaths.size(); i++) {
                String pageText = imageDocumentHandler.extractText(imagePaths.get(i), language);
                fullText.append(pageText);
                
                // Añadir separador entre páginas
                if (i < imagePaths.size() - 1) {
                    fullText.append("\n\n--- Página ").append(i + 2).append(" ---\n\n");
                }
                
                logger.debug("OCR completado para la página " + (i + 1));
            }
            
            logger.info("Extracción OCR completada para todas las páginas del PDF");
            return fullText.toString();
            
        } finally {
            // Limpieza: eliminar archivos temporales
            deleteDirectory(tempDir.toFile());
        }
    }
    
    /**
     * Elimina recursivamente un directorio y su contenido
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}