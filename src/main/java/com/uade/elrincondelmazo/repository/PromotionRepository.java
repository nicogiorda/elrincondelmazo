package com.uade.elrincondelmazo.repository;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.uade.elrincondelmazo.entity.Promotion;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
     List<Promotion> findByActiveTrue();

     @Override
     @EntityGraph(attributePaths = {"collection", "product"})
     List<Promotion> findAll();

     @EntityGraph(attributePaths = {"collection", "product"})
     @Query("""
             select p from Promotion p
             where p.active = true and p.startDate <= :now
               and (p.endDate is null or p.endDate >= :now)
             order by p.id
             """)
     List<Promotion> findPublicAt(@Param("now") LocalDateTime now);

}

//findbyactivetrue equivale conceptualmente a:SELECT * FROM promotion WHERE active = true;

