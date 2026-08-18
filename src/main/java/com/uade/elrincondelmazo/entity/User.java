package com.uade.elrincondelmazo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
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
