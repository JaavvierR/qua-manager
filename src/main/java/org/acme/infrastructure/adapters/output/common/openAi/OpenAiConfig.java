package org.acme.infrastructure.adapters.output.common.openAi;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.acme.infrastructure.providers.qualifers.AIProviderQualifiers.OpenAI;

@ApplicationScoped
@OpenAI
public class OpenAiConfig {
    
    @ConfigProperty(name = "quarkus.langchain4j.openai.api-key")
    String apiKey;
    
    @ConfigProperty(name = "quarkus.langchain4j.openai.api-url", defaultValue = "https://api.openai.com/v1/chat/completions")
    private String API_URL;
    
    @ConfigProperty(name = "quarkus.langchain4j.openai.model-name", defaultValue = "gpt-3.5-turbo")
    private String MODEL_NAME;
    
    @ConfigProperty(name = "quarkus.langchain4j.openai.system-prompt", defaultValue = "Eres un asistente útil.")
    private String SYSTEM_PROMPT;
    
    @ConfigProperty(name = "quarkus.langchain4j.openai.default-temperature", defaultValue = "0.7")
    private double DEFAULT_TEMPERATURE;
    
    public String getApiKey() {
        return apiKey;
    }
    
    public String getApiUrl() {
        return API_URL;
    }
    
    public String getModelName() {
        return MODEL_NAME;
    }
    
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }
    
    public double getDefaultTemperature() {
        return DEFAULT_TEMPERATURE;
    }
}