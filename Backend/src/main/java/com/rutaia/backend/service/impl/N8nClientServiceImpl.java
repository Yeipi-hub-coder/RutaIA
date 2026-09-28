package com.rutaia.backend.service.impl;compararPreguntas

import com.rutaia.backend.config.N8nProperties;
import com.rutaia.backend.dto.Request.N8nRequestDTO;
import com.rutaia.backend.dto.Response.N8nResponseDTO;
import com.rutaia.backend.service.N8nClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class N8nClientServiceImpl implements N8nClientService {

    private final RestClient restClient;
    private final N8nProperties n8nProperties;

    @Override
    public N8nResponseDTO ejecutarFlujoRag(N8nRequestDTO request) {
        try {
            return restClient.post()
                    .uri(n8nProperties.getWebhookUrl())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(N8nResponseDTO.class);
        } catch (RestClientException ex) {
            // Cubre timeouts, conexion rechazada, respuestas 4xx/5xx del workflow, etc. (RN10)
            log.error("Error llamando al webhook de n8n: {}", ex.getMessage());
            return N8nResponseDTO.builder()
                    .estado("ERROR")
                    .respuesta("No fue posible generar una recomendacion en este momento.")
                    .fuentes(java.util.List.of())
                    .build();
        }
    }

    @Override
    public N8nResponseDTO compararPreguntas(N8nRequestDTO request1, N8nRequestDTO request2) {
        try {
            return restClient.post()
                    .uri(n8nProperties.getWebhookUrl())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(request1)
                    .retrieve()
                    .body(N8nResponseDTO.class);
        } catch ()
    }

}
