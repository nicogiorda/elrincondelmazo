package com.uade.elrincondelmazo.entity;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column
    private String description;

}
