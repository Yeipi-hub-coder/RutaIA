package com.rutaia.backend.dto.Response;

import com.rutaia.backend.dto.Response.FuenteResponseDTO;
import com.rutaia.backend.model.enums.EstadoFinal;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecomendacionResponseDTO {
    private Integer consultaId;
    private String pregunta;
    private String respuesta;
    private List<FuenteResponseDTO> fuentes;
    private EstadoFinal estado;
    private LocalDateTime fecha;
    private String nivelExperiencia;
}
