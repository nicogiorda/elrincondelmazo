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
    // Repositorio para acceder a la entidad Collection en la base de datos
    @Autowired
    private CollectionRepository collectionRepository;

    /*
     * Servicio que implementa operaciones CRUD simples para las colecciones:
     * - Obtener todas las colecciones
     * - Obtener una colección por id
     * - Crear, actualizar y eliminar colecciones
     *
     * Los métodos lanzan `ResourceNotFoundException` cuando corresponda y
     * delegan la persistencia en `collectionRepository`.
     */ 

    @Override
    public List<Collection> getAllCollections() {
        // Devuelve todas las colecciones almacenadas
        return collectionRepository.findAll();
    }

    @Override
    public Collection getCollectionById(Long id) {
        // Busca una colección por id; si no existe lanza ResourceNotFoundException
        return collectionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Colección no encontrada con id: " + id));
    }

    @Override
    public Collection createCollection(CollectionRequest request) {

        // Crea una nueva entidad Collection a partir del DTO request
        Collection collection = new Collection();

        collection.setName(request.getName());
        collection.setDescription(request.getDescription());

        // Persiste la nueva colección y devuelve la entidad guardada
        return collectionRepository.save(collection);
    }

    @Override
    public Collection updateCollection(Long id, CollectionRequest request) {

        // Recupera la colección existente (lanza excepción si no existe)
        Collection collection = getCollectionById(id);

        // Actualiza campos modificables
        collection.setName(request.getName());
        collection.setDescription(request.getDescription());

        // Guarda y devuelve la colección actualizada
        return collectionRepository.save(collection);
    }

    @Override
    public void deleteCollection(Long id) {

        // Recupera la colección para asegurar existencia y luego la elimina
        Collection collection = getCollectionById(id);
        collectionRepository.delete(collection);
    }
}
