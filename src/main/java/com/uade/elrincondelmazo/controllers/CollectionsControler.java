package com.uade.elrincondelmazo.controllers;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uade.elrincondelmazo.entity.dto.CollectionResponse;
import com.uade.elrincondelmazo.service.CollectionService;

import java.net.URI;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.uade.elrincondelmazo.entity.Collection;
import com.uade.elrincondelmazo.entity.dto.CollectionRequest;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/collections")
public class CollectionsControler {

    @Autowired
    private CollectionService collectionService;

    @GetMapping
    public ResponseEntity<List<CollectionResponse>> getAllCollections() {

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
        Collection collection =
                collectionService.getCollectionById(id);

        return ResponseEntity.ok(
                CollectionResponse.fromCollection(collection)
        );
    }

    @PostMapping
    public ResponseEntity<CollectionResponse> createCollection(
            @Valid @RequestBody CollectionRequest request
    ) {
        Collection collection =
                collectionService.createCollection(request);

        URI location = URI.create("/collections/" + collection.getId());

        return ResponseEntity
                .created(location)
                .body(CollectionResponse.fromCollection(collection));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CollectionResponse> updateCollection(
            @PathVariable Long id,
            @Valid @RequestBody CollectionRequest request
    ) {
        Collection collection =
                collectionService.updateCollection(id, request);

        return ResponseEntity.ok(
                CollectionResponse.fromCollection(collection)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollection(
            @PathVariable Long id
    ) {
        collectionService.deleteCollection(id);

        return ResponseEntity.noContent().build();
    }
}
