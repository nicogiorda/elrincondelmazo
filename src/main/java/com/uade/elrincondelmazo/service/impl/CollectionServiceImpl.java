package com.uade.elrincondelmazo.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.uade.elrincondelmazo.entity.Collection;
import com.uade.elrincondelmazo.repository.CollectionRepository;
import com.uade.elrincondelmazo.service.CollectionService;

@Service
public class CollectionServiceImpl implements CollectionService {
    @Autowired
    private CollectionRepository collectionRepository;

    @Override
    public List<Collection> getAllCollections() {
        return collectionRepository.findAll();
    }

}
