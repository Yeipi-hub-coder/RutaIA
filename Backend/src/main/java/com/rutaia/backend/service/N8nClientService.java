package com.rutaia.backend.service;

import com.rutaia.backend.dto.Request.N8nRequestDTO;
import com.rutaia.backend.dto.Response.N8nResponseDTO;

public interface N8nClientService {
    // Llama al webhook de n8n que dispara el flujo RAG (embedding + Qdrant + OpenRouter)
    N8nResponseDTO ejecutarFlujoRag(N8nRequestDTO request);
    N8nResponseDTO compararPreguntas(N8nRequestDTO request1, N8nRequestDTO request2);
}
