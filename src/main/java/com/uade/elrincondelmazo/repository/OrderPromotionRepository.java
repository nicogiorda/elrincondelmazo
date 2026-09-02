package com.uade.elrincondelmazo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.elrincondelmazo.entity.OrderPromotion;

import java.util.List;

@Repository
public interface OrderPromotionRepository extends JpaRepository<OrderPromotion, Long> {

    List<OrderPromotion> findByOrderId(Long orderId);

}

