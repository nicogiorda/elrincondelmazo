package com.uade.elrincondelmazo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.elrincondelmazo.entity.Cart;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /// El CRUD basico se hereda de JpaRepository, pero podemos agregar metodos
    /// personalizados si es necesario

    Optional<Cart> findByUserId(Long userId);

}
