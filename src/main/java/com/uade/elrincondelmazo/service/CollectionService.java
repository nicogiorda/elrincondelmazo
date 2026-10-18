package com.uade.elrincondelmazo.service;

import java.util.List;

import com.uade.elrincondelmazo.entity.Collection;
import com.uade.elrincondelmazo.entity.dto.CollectionRequest;

public interface CollectionService {
    /*
     * Interfaz para operaciones CRUD sobre la entidad `Collection`.
     * Implementaciones deben encargarse de la persistencia y de lanzar
     * excepciones apropiadas cuando una colección no exista.
     */

    // Devuelve todas las colecciones disponibles
    List<Collection> getAllCollections();

    // Obtiene una colección por su id (debe lanzar excepción si no existe)
    Collection getCollectionById(Long id);

    // Crea una nueva colección a partir del DTO de request
    Collection createCollection(CollectionRequest request);

    // Actualiza la colección indicada con los datos del request
    Collection updateCollection(Long id, CollectionRequest request);

    // Elimina la colección indicada (debe validar existencia antes de borrar)
    void deleteCollection(Long id);

}
