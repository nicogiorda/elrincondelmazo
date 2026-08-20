package com.uade.elrincondelmazo.entity;


import com.uade.elrincondelmazo.enums.PaymentMethod;
import com.uade.elrincondelmazo.enums.StateOrder;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Data
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column
    private StateOrder state;

    @Enumerated(EnumType.STRING)
    @Column
    private PaymentMethod paymentMethod;

    @Column
    private BigDecimal subtotal;

    @Column
    private BigDecimal descuentoTotal;

    @Column
    private BigDecimal total;

}
