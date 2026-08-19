package com.uade.elrincondelmazo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.elrincondelmazo.entity.Cart;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    
}
