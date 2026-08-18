package com.uade.elrincondelmazo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private Double price;

    @Column
    private String type;

    @Column
    private String image_url;

    @Column
    private int stock;

    @Column
    private String status;

    @Column
    private Date  created_at;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collection_id", nullable = false)
    private Collection collection;
}
