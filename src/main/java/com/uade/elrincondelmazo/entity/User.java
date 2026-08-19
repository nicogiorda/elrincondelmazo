package com.uade.elrincondelmazo.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "users") // Especifico el nombre de la tabla en la base de datos, lo recomienda supabase para evitar problemas con palabras reservadas
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column (unique = true, nullable = false)
    private String username;

    @Column (unique = true, nullable = false)
    private String email;

    @Column (nullable = false)
    private String password_hash;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @Column
    private String role;

    @Column
    private Date createdAt;

    @Column
    private Boolean active;



}
