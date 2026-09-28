package com.rutaia.backend.dto.Response;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FuenteResponseDTO {
    private Integer cursoId;
    private String nombre;
    private String descripcion;
    private String categoria;
    private BigDecimal similitud;
    private String nivelExperiencia;
}
