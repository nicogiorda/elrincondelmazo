package com.uade.elrincondelmazo.controllers;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uade.elrincondelmazo.entity.dto.CollectionResponse;
import com.uade.elrincondelmazo.service.CollectionService;

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
}
