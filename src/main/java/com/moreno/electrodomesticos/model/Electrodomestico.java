package com.moreno.electrodomesticos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "electrodomesticos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Electrodomestico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "clasificacion_energetica")
    private String clasificacionEnergetica;

    @Column(length = 100)
    private String dimensiones;

    @Column(name = "especificaciones_principales", length = 500)
    private String especificacionesPrincipales;
}
