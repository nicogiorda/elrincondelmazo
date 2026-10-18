package com.uade.elrincondelmazo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Collection {

    /*
     * Entidad que representa una colección de productos.
     * Campos:
     * - `id`: identificador único generado por la BD
     * - `name`: nombre único y no nulo de la colección
     * - `description`: descripción opcional para mostrar información adicional
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identificador único de la colección

    @Column(unique = true, nullable = false)
    private String name; // Nombre único y obligatorio

    @Column(nullable = true)
    private String description; // Descripción opcional

}
