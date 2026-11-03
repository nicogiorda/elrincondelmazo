package com.uade.elrincondelmazo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.uade.elrincondelmazo.enums.StateOrder;

import com.uade.elrincondelmazo.entity.OrderItem;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    ///Las operaciones CRUD básicas ya están implementadas por JpaRepository, junto con varias operaciones mas,
    ///Pero las operaciones especificas deben implementarse manualmente.
    List<OrderItem> findByOrderId(Long orderId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"order", "product"})
    List<OrderItem> findByOrder_User_IdOrderByIdDesc(Long userId);

    java.util.Optional<OrderItem> findByIdAndOrder_User_Id(Long id, Long userId);

    boolean existsByOrder_User_IdAndProduct_IdAndOrder_StateNot(
        Long userId,
        Long productId,
        StateOrder state
);

}
