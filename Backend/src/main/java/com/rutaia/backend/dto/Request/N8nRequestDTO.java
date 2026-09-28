package com.rutaia.backend.dto.Request;

import lombok.*;

// Lo que Spring Boot envia a n8n
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class N8nRequestDTO {
    private Integer consultaId;
    private String pregunta;
    private String nivelExperiencia;  //AQUI  se envia el nivel de experiencia
    private String areaInteres;
}
