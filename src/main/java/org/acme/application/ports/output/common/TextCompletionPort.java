package org.acme.application.ports.output.common;

import io.smallrye.mutiny.Uni;

public interface TextCompletionPort {

    // --- Generación de texto ---
    Uni<String> generateResponseAsync(String validationPrompt);
    /**
     * Genera una completación de texto basada en una solicitud y una plantilla.
     *
     * @param request
     * @param promptTemplate
     * @return 
     */
    //TextCompletion generateCompletion(TextRequest request, String promptTemplate);

    /**
     * Envía un mensaje al modelo de IA y recibe una respuesta como string.
     *
     * @param prompt El mensaje a enviar
     * @return La respuesta del modelo
     */

    String getModelName();

    // --- Métricas de ejecución ---

    /**
     * Inicia el temporizador para medir el tiempo de procesamiento.
     */
    void startTimer();
    
    void pauseTimer();

    /**
     * Obtiene el tiempo transcurrido.
     *
     * @return Tiempo transcurrido en segundos
     */
    double getElapsedTime();

    // --- Métodos adicionales que estaban en AIModelPort ---

    /**
     * Extrae entidades de una respuesta del modelo.
     *
     * @param response Respuesta del modelo
     * @return Entidades extraídas
     */

}