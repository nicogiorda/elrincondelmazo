package com.uade.elrincondelmazo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.uade.elrincondelmazo.entity.Collection;

public interface CollectionRepository extends JpaRepository<Collection, Long> {
    
}
