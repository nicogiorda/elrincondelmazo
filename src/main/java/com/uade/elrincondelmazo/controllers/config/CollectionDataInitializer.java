package com.uade.elrincondelmazo.controllers.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.uade.elrincondelmazo.entity.Collection;
import com.uade.elrincondelmazo.repository.CollectionRepository;

@Component
public class CollectionDataInitializer implements CommandLineRunner {

    private final CollectionRepository collectionRepository;

    public CollectionDataInitializer(CollectionRepository collectionRepository) {
        this.collectionRepository = collectionRepository;
    }

    @Override
    public void run(String... args) {

        createCollectionIfNotExists("NBA");
        createCollectionIfNotExists("My Little Pony");
        createCollectionIfNotExists("Cars");
        createCollectionIfNotExists("Marvel");
    }

    private void createCollectionIfNotExists(String name) {

        if (!collectionRepository.existsByName(name)) {

            Collection collection = new Collection();
            collection.setName(name);

            collectionRepository.save(collection);
        }
    }
}
