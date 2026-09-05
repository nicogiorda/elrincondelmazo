package com.uade.elrincondelmazo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.elrincondelmazo.entity.Order;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    ///Las operaciones CRUD básicas ya están implementadas por JpaRepository, junto con varias operaciones mas,
    ///Pero las operaciones especificas deben implementarse manualmente.
    List<Order> findByUserId(Long userId);

    Optional<Order> findByIdAndUserId(Long orderId, Long userId);

}
