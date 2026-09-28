package com.rutaia.backend.repository;

import com.rutaia.backend.model.Recomendacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecomendacionRepository extends JpaRepository<Recomendacion, Integer> {
    Optional<Recomendacion> findByConsultaId(Integer consultaId);

    // Trae, en un solo SELECT, la recomendacion + su consulta + su estudiante + sus fuentes + el curso
    // de cada fuente. Evita el problema N+1 al armar el historial de un estudiante.
    @Query("""
        select distinct r from Recomendacion r
        join fetch r.consulta c
        join fetch c.estudiante
        left join fetch r.fuentes f
        left join fetch f.curso
        where c.estudiante.id = :estudianteId
        order by c.fecha asc
        """)
    List<Recomendacion> findConEstudianteId(@Param("estudianteId") Integer estudianteId);

    // Igual que la anterior pero sin filtrar por estudiante, para listar todas las consultas.
    @Query("""
        select distinct r from Recomendacion r
        join fetch r.consulta c
        join fetch c.estudiante
        left join fetch r.fuentes f
        left join fetch f.curso
        order by c.fecha desc
        """)
    List<Recomendacion> findTodasConDetalle();

    @Query("""
        select f.curso.id, f.curso.nombre, count(f)
        from Fuente f
        group by f.curso.id, f.curso.nombre
        order by count(f) desc
        """)
    List<Object[]> cursoMasRecomendado();
}
