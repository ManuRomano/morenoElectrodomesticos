package com.moreno.electrodomesticos.service;

import com.moreno.electrodomesticos.model.Electrodomestico;
import com.moreno.electrodomesticos.repository.ElectrodomesticoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ElectrodomesticoService {

    private static final int PAGE_SIZE = 25;

    private final ElectrodomesticoRepository repository;

    public ElectrodomesticoService(ElectrodomesticoRepository repository) {
        this.repository = repository;
    }

    public Page<Electrodomestico> findPaginated(int page, String tipo, String marca) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("marca", "modelo"));
        boolean hasTipo = tipo != null && !tipo.isBlank() && !tipo.equals("Todos");
        boolean hasMarca = marca != null && !marca.isBlank() && !marca.equals("Todas");

        if (hasTipo && hasMarca) {
            return repository.findByTipoIgnoreCaseAndMarcaIgnoreCase(tipo, marca, pageable);
        } else if (hasTipo) {
            return repository.findByTipoIgnoreCase(tipo, pageable);
        } else if (hasMarca) {
            return repository.findByMarcaIgnoreCase(marca, pageable);
        }
        return repository.findAll(pageable);
    }

    public Electrodomestico save(Electrodomestico e) {
        return repository.save(e);
    }

    public void delete(Electrodomestico e) {
        repository.delete(e);
    }

    public void deleteAll(List<Electrodomestico> list) {
        repository.deleteAll(list);
    }

    public Optional<Electrodomestico> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<String> findTipos() {
        return repository.findDistinctTipos();
    }

    @Transactional(readOnly = true)
    public List<String> findMarcas() {
        return repository.findDistinctMarcas();
    }

    @Transactional(readOnly = true)
    public List<String> findMarcasByTipo(String tipo) {
        if (tipo == null || tipo.isBlank() || tipo.equals("Todos")) {
            return repository.findDistinctMarcas();
        }
        return repository.findDistinctMarcasByTipo(tipo);
    }
}
