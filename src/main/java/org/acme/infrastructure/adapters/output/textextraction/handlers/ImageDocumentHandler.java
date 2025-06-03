package org.acme.infrastructure.adapters.output.textextraction.handlers;

import org.acme.domain.textextraction.exception.TextExtractionException;
import org.acme.infrastructure.adapters.output.textextraction.config.TesseractConfig;
import net.sourceforge.tess4j.Tesseract;
import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import jakarta.inject.Singleton;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

@Singleton
public class ImageDocumentHandler {
    
    private static final Logger logger = Logger.getLogger(ImageDocumentHandler.class);
    
    private final TesseractConfig tesseractConfig;
    
    @Inject
    public ImageDocumentHandler(TesseractConfig tesseractConfig) {
        this.tesseractConfig = tesseractConfig;
        logger.info("ImageDocumentHandler inicializado con configuración de Tesseract");
    }
    
    /**
     * Preprocesa la imagen para mejorar la precisión del OCR
     * Implementa técnicas similares a las del script Python
     */
    private BufferedImage preprocessImage(File imageFile) throws Exception {
        logger.debug("Preprocesando imagen: " + imageFile.getPath());
        
        // Cargar la imagen
        BufferedImage originalImage = ImageIO.read(imageFile);
        if (originalImage == null) {
            throw new Exception("No se pudo cargar la imagen");
        }
        
        // 1. Convertir a escala de grises
        BufferedImage grayscaleImage = new BufferedImage(
            originalImage.getWidth(), 
            originalImage.getHeight(), 
            BufferedImage.TYPE_BYTE_GRAY);
        
        Graphics2D g = grayscaleImage.createGraphics();
        g.drawImage(originalImage, 0, 0, null);
        g.dispose();
        
        // 2. Aumentar el contraste
        BufferedImage contrastedImage = enhanceContrast(grayscaleImage, 2.0f);
        
        // 3. Aumentar tamaño (subir resolución)
        int newWidth = originalImage.getWidth() * 2;
        int newHeight = originalImage.getHeight() * 2;
        
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = resizedImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(contrastedImage, 0, 0, newWidth, newHeight, null);
        g2.dispose();
        
        logger.info("Preprocesamiento de imagen completado exitosamente");
        return resizedImage;
    }
    
    /**
     * Método para aumentar el contraste de la imagen
     */
    private BufferedImage enhanceContrast(BufferedImage image, float contrastFactor) {
        logger.debug("Mejorando contraste con factor: " + contrastFactor);
        
        BufferedImage resultImage = new BufferedImage(
            image.getWidth(), 
            image.getHeight(), 
            image.getType());
        
        // Algoritmo simple de ajuste de contraste
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                Color color = new Color(pixel);
                
                // Extraer valor de gris
                int grayValue = color.getRed();
                
                // Aplicar ajuste de contraste
                float newValue = (grayValue / 255.0f - 0.5f) * contrastFactor + 0.5f;
                newValue = Math.max(0, Math.min(1, newValue)) * 255;
                
                int newGrayValue = Math.round(newValue);
                Color newColor = new Color(newGrayValue, newGrayValue, newGrayValue);
                resultImage.setRGB(x, y, newColor.getRGB());
            }
        }
        
        return resultImage;
    }
    
    /**
     * Extrae el texto de una imagen aplicando preprocesamiento
     */
    public String extractText(Path filePath, String language) {
        logger.debug("Extrayendo texto de imagen: " + filePath);
        
        try {
            File imageFile = filePath.toFile();
            
            // Preprocesar la imagen antes del OCR
            BufferedImage processedImage = preprocessImage(imageFile);
            
            // Configurar Tesseract
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tesseractConfig.getDataPath());
            logger.debug("Ruta de datos de Tesseract configurada: " + tesseractConfig.getDataPath());
            
            // Configurar idioma para OCR
            if (language != null && !language.isEmpty()) {
                tesseract.setLanguage(language);
                logger.debug("Configurado idioma OCR: " + language);
            } else {
                tesseract.setLanguage(tesseractConfig.getDefaultLanguage());
                logger.debug("Configurado idioma OCR predeterminado: " + tesseractConfig.getDefaultLanguage());
            }
            
            tesseract.setOcrEngineMode(1);
            tesseract.setPageSegMode(3);
            
            // Ejecutar OCR en la imagen preprocesada
            String text = tesseract.doOCR(processedImage);
            logger.info("Texto extraído exitosamente de la imagen");
            return text;
            
        } catch (Exception e) {
            logger.error("Error al extraer texto de la imagen", e);
            throw new TextExtractionException("Error al extraer texto de la imagen: " + e.getMessage(), e);
        }
    }
}