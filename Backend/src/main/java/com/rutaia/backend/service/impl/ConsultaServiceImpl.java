package com.rutaia.backend.service.impl;

import com.rutaia.backend.dto.Request.ConsultaRequestDTO;
import com.rutaia.backend.dto.Response.ConsultaResponseDTO;
import com.rutaia.backend.dto.n8n.N8nFuenteDTO;
import com.rutaia.backend.dto.Request.N8nRequestDTO;
import com.rutaia.backend.dto.Response.N8nResponseDTO;
import com.rutaia.backend.dto.Response.FuenteResponseDTO;
import com.rutaia.backend.dto.Response.RecomendacionResponseDTO;
import com.rutaia.backend.model.*;
import com.rutaia.backend.model.enums.EstadoConsulta;
import com.rutaia.backend.model.enums.EstadoFinal;
import com.rutaia.backend.exception.BusinessException;
import com.rutaia.backend.exception.ResourceNotFoundException;
import com.rutaia.backend.repository.*;
import com.rutaia.backend.service.ConsultaService;
import com.rutaia.backend.service.N8nClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultaServiceImpl implements ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final EstudianteRepository estudianteRepository;
    private final CursoRepository cursoRepository;
    private final RecomendacionRepository recomendacionRepository;
    private final FuenteRepository fuenteRepository;
    private final N8nClientService n8nClientService;

    @Override
    @Transactional
    public ConsultaResponseDTO procesarConsulta(ConsultaRequestDTO dto) {

        // las preguntas vacias no se mandan a n8n (la validacion @NotBlank ya lo cubre en el controller,
        // esto es una segunda barrera a nivel de servicio)
        if (!StringUtils.hasText(dto.getPregunta())) {
            throw new BusinessException("La pregunta no puede estar vacia");
        }

        // funcionalidad: toda consulta debe pertenecer a un estudiante existente
        Estudiante estudiante = estudianteRepository.findById(dto.getEstudianteId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un estudiante con id " + dto.getEstudianteId()));

        // funcionalidad: se guarda la consulta con estado PENDIENTE antes de llamar a n8n
        Consulta consulta = Consulta.builder()
                .estudiante(estudiante)
                .pregunta(dto.getPregunta())
                .estado(EstadoConsulta.PENDIENTE)
                .build();
        consulta = consultaRepository.save(consulta);

        // funcionalidad: SpringBoot es el intermediario obligatorio hacia n8n
        N8nRequestDTO n8nRequest = N8nRequestDTO.builder()
                .consultaId(consulta.getId())
                .pregunta(consulta.getPregunta())
                .nivelExperiencia(estudiante.getNivelExperiencia().name())
                .areaInteres(estudiante.getAreaInteres())
                .build();

        N8nResponseDTO n8nResponse = n8nClientService.ejecutarFlujoRag(n8nRequest);

        EstadoFinal estadoFinal = mapEstado(n8nResponse.getEstado());

        // funcionalidad: se guarda la recomendacion (aunque sea SIN_RESULTADOS o ERROR, para dejar trazabilidad)
        Recomendacion recomendacion = Recomendacion.builder()
                .consulta(consulta)
                .respuestaGenerada(n8nResponse.getRespuesta() != null
                        ? n8nResponse.getRespuesta()
                        : "No se encontro informacion suficiente en el catalogo para responder tu pregunta.")
                .estadoFinal(estadoFinal)
                .build();
        recomendacion = recomendacionRepository.save(recomendacion);

        if (n8nResponse.getFuentes() != null) {
            for (N8nFuenteDTO f : n8nResponse.getFuentes()) {
                // funcionalidad: los cursos mostrados como fuente deben existir en la base relacional
                Curso curso = cursoRepository.findById(f.getCursoId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "El curso fuente con id " + f.getCursoId() + " no existe"));

                Fuente fuente = Fuente.builder()
                        .recomendacion(recomendacion)
                        .curso(curso)
                        .similitud(f.getSimilitud())
                        .build();
                fuenteRepository.save(fuente);
                recomendacion.getFuentes().add(fuente);
            }
        }

        //funcionalidad: actualiza el estado final de la consulta
        consulta.setEstado(estadoToConsultaEstado(estadoFinal));
        consulta = consultaRepository.save(consulta);

        return DtoToConsultaResponse(consulta, recomendacion);
    }

    @Override
    public List<ConsultaResponseDTO> listar() {
        // Un solo SELECT con JOIN FETCH en vez de una consulta extra por cada
        // consulta/fuente/curso (mismo problema N+1 que tenia el historial por estudiante).
        return recomendacionRepository.findTodasConDetalle().stream()
                .map(r -> DtoToConsultaResponse(r.getConsulta(), r))
                .toList();
    }

    @Override
    public List<ConsultaResponseDTO> historial(Integer id) {
        buscarOFallar(id); // valida que el estudiante exista
        return recomendacionRepository.findConEstudianteId(id).stream()
                .map(r -> DtoToConsultaResponse(r.getConsulta(), r))
                .toList();
    }

    @Override
    public ConsultaResponseDTO obtenerPorId(Integer id) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una consulta con id " + id));
        Recomendacion recomendacion = recomendacionRepository.findByConsultaId(id).orElse(null);
        return DtoToConsultaResponse(consulta, recomendacion);
    }

    private EstadoFinal mapEstado(String estado) {
        try {
            return EstadoFinal.valueOf(estado);
        } catch (Exception e) {
            return EstadoFinal.ERROR;
        }
    }

    private EstadoConsulta estadoToConsultaEstado(EstadoFinal estadoFinal) {
        return switch (estadoFinal) {
            case RESPONDIDA -> EstadoConsulta.RESPONDIDA;
            case SIN_RESULTADOS -> EstadoConsulta.SIN_RESULTADOS;
            case ERROR -> EstadoConsulta.ERROR;
        };
    }

    private ConsultaResponseDTO DtoToConsultaResponse(Consulta consulta, Recomendacion recomendacion) {
        RecomendacionResponseDTO recDto = null;
        if (recomendacion != null) {
            List<FuenteResponseDTO> fuentes = recomendacion.getFuentes().stream()
                    .map(f -> FuenteResponseDTO.builder()
                            .cursoId(f.getCurso().getId())
                            .nombre(f.getCurso().getNombre())
                            .descripcion(f.getCurso().getDescripcion())
                            .categoria(f.getCurso().getCategoria())
                            .similitud(f.getSimilitud())
                            .build())
                    .toList();

            recDto = RecomendacionResponseDTO.builder()
                    .consultaId(consulta.getId())
                    .pregunta(consulta.getPregunta())
                    .respuesta(recomendacion.getRespuestaGenerada())
                    .fuentes(fuentes)
                    .estado(recomendacion.getEstadoFinal())
                    .fecha(recomendacion.getFecha())
                    .build();
        }

        return ConsultaResponseDTO.builder()
                .id(consulta.getId())
                .estudianteId(consulta.getEstudiante().getId())
                .pregunta(consulta.getPregunta())
                .fecha(consulta.getFecha())
                .estado(consulta.getEstado())
                .recomendacion(recDto)
                .build();
    }

    private Estudiante buscarOFallar(Integer id) {
        return estudianteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un estudiante con id " + id));
    }
}
