package com.uade.elrincondelmazo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.uade.elrincondelmazo.enums.PaymentMethod;
import com.uade.elrincondelmazo.enums.StateOrder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //No usamos referenced column id porque al no especificar una columna, hibernate referencia automaticamente a la primary key de User
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StateOrder state;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Column
    private BigDecimal subtotal;

    @Column(name = "total_discount", nullable = false)
    private BigDecimal totalDiscount;

    @Column(nullable = false)
    private BigDecimal total;

}
