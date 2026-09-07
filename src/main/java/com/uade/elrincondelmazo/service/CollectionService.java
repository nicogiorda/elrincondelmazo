package com.uade.elrincondelmazo.service;

import java.util.List;

import com.uade.elrincondelmazo.entity.Collection;
import com.uade.elrincondelmazo.entity.dto.CollectionRequest;

public interface CollectionService {
    List<Collection> getAllCollections();

    Collection getCollectionById(Long id);

    Collection createCollection(CollectionRequest request);

    Collection updateCollection(Long id, CollectionRequest request);

    void deleteCollection(Long id);


}
