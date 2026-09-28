package com.rutaia.backend.dto.Response;

import com.rutaia.backend.dto.n8n.N8nFuenteDTO;
import lombok.*;

import java.util.List;

// Lo que n8n devuelve a Spring Boot luego de ejecutar el flujo RAG (RF13)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class N8nResponseDTO {
    private String estado;
    private String respuesta;
    private List<N8nFuenteDTO> fuentes;
    private String nivelExperiencia;
}
