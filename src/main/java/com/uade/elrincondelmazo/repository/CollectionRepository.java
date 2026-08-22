package com.uade.elrincondelmazo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.elrincondelmazo.entity.Collection;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {
    

}
