package com.moreno.electrodomesticos.repository;

import com.moreno.electrodomesticos.model.Electrodomestico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElectrodomesticoRepository extends JpaRepository<Electrodomestico, Long> {

    Page<Electrodomestico> findAll(Pageable pageable);

    Page<Electrodomestico> findByTipoIgnoreCase(String tipo, Pageable pageable);

    Page<Electrodomestico> findByMarcaIgnoreCase(String marca, Pageable pageable);

    Page<Electrodomestico> findByTipoIgnoreCaseAndMarcaIgnoreCase(String tipo, String marca, Pageable pageable);

    @Query("SELECT DISTINCT e.tipo FROM Electrodomestico e ORDER BY e.tipo")
    List<String> findDistinctTipos();

    @Query("SELECT DISTINCT e.marca FROM Electrodomestico e ORDER BY e.marca")
    List<String> findDistinctMarcas();

    @Query("SELECT DISTINCT e.marca FROM Electrodomestico e WHERE LOWER(e.tipo) = LOWER(:tipo) ORDER BY e.marca")
    List<String> findDistinctMarcasByTipo(@Param("tipo") String tipo);
}
