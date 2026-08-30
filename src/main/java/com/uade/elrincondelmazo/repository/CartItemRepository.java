package com.uade.elrincondelmazo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.elrincondelmazo.entity.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /// El CRUD basico viene heredado de JpaRepository

    Optional<CartItem> findByCart_IdAndProduct_Id(Long cartId, Long productId);

    List<CartItem> findByCart_Id(Long cartId);

}
