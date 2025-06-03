package org.acme.infrastructure.adapters.output.textextraction.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TesseractConfig {
    
    @ConfigProperty(name = "tess4j.datapath")
    private String dataPath;
    
    @ConfigProperty(name = "tess4j.language", defaultValue = "tesseract_es")
    private String defaultLanguage;
    
    public String getDataPath() {
        return dataPath;
    }
    
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
}