package com.uade.elrincondelmazo.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;


@Entity
@Data
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
    - Relacion obligatoria porque no deberia existir un carrito sin User
    - Relacion OneToOne porque un User solo puede tener un carrito
    - unique = true para que un User no pueda tener mas de un carrito
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private LocalDateTime createdAt;

}
