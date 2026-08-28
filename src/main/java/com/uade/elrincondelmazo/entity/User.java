package com.uade.elrincondelmazo.entity;

import java.time.LocalDateTime;


import com.uade.elrincondelmazo.enums.Role;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users") // Especifico el nombre de la tabla en la base de datos, lo recomienda supabase para evitar problemas con palabras reservadas
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (unique = true, nullable = false)
    private String email;


    @Column (name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Boolean active;

}
