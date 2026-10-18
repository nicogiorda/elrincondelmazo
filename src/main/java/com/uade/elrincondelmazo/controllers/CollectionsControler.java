package com.uade.elrincondelmazo.controllers;
import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uade.elrincondelmazo.entity.Collection;
import com.uade.elrincondelmazo.entity.dto.CollectionRequest;
import com.uade.elrincondelmazo.entity.dto.CollectionResponse;
import com.uade.elrincondelmazo.service.CollectionService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/collections")
public class CollectionsControler {

        // Controlador REST para operaciones CRUD sobre colecciones
        // Rutas base: /collections
        @Autowired
        private CollectionService collectionService;

    @GetMapping
    public ResponseEntity<List<CollectionResponse>> getAllCollections() {
                // Recupera todas las colecciones y las mapea a DTOs para la respuesta
                List<CollectionResponse> collections = collectionService.getAllCollections()
                        .stream()
                        .map(CollectionResponse::fromCollection)
                        .toList();

                return ResponseEntity.ok(collections);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollectionResponse> getCollectionById(
            @PathVariable Long id
    ) {
        // Obtiene una colección por id y devuelve su DTO
        Collection collection = collectionService.getCollectionById(id);

        return ResponseEntity.ok(CollectionResponse.fromCollection(collection));
    }

    @PostMapping
    public ResponseEntity<CollectionResponse> createCollection(
            @Valid @RequestBody CollectionRequest request
    ) {
        // Crea una nueva colección a partir del DTO request
        Collection collection = collectionService.createCollection(request);

        // Construye la URI del recurso creado y responde 201 Created
        URI location = URI.create("/collections/" + collection.getId());

        return ResponseEntity.created(location).body(CollectionResponse.fromCollection(collection));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CollectionResponse> updateCollection(
            @PathVariable Long id,
            @Valid @RequestBody CollectionRequest request
    ) {
        // Actualiza una colección existente y devuelve la versión actualizada
        Collection collection = collectionService.updateCollection(id, request);

        return ResponseEntity.ok(CollectionResponse.fromCollection(collection));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCollection(
            @PathVariable Long id
    ) {
        collectionService.deleteCollection(id);

        return ResponseEntity.ok("Colección eliminada correctamente");
    }
}
