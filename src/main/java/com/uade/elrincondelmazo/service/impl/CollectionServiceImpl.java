package com.uade.elrincondelmazo.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.uade.elrincondelmazo.entity.Collection;
import com.uade.elrincondelmazo.entity.dto.CollectionRequest;
import com.uade.elrincondelmazo.exception.ResourceNotFoundException;
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

    @Override
    public Collection getCollectionById(Long id) {
        return collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Colección no encontrada con id: " + id));
    }

    @Override
    public Collection createCollection(CollectionRequest request) {

        Collection collection = new Collection();

        collection.setName(request.getName());
        collection.setDescription(request.getDescription());

        return collectionRepository.save(collection);
    }

    @Override
    public Collection updateCollection(Long id, CollectionRequest request) {

        Collection collection = getCollectionById(id);

        collection.setName(request.getName());
        collection.setDescription(request.getDescription());

        return collectionRepository.save(collection);
    }

    @Override
    public void deleteCollection(Long id) {

        Collection collection = getCollectionById(id);

        collectionRepository.delete(collection);
    }
}
