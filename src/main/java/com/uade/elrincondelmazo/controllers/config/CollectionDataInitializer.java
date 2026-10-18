package com.uade.elrincondelmazo.controllers.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.uade.elrincondelmazo.entity.Collection;
import com.uade.elrincondelmazo.repository.CollectionRepository;

@Component
public class CollectionDataInitializer implements CommandLineRunner {
    // Inicializador de datos que se ejecuta al arrancar la aplicación.
    // Inserta colecciones base si no existen en la base de datos.
    private final CollectionRepository collectionRepository;

    public CollectionDataInitializer(CollectionRepository collectionRepository) {
        this.collectionRepository = collectionRepository;
    }

    @Override
    public void run(String... args) {

        // Nombres de ejemplo que se crearán si no existen
        createCollectionIfNotExists("NBA");
        createCollectionIfNotExists("My Little Pony");
        createCollectionIfNotExists("Cars");
        createCollectionIfNotExists("Marvel");
    }

    // Helper que crea una colección solo si no existe una con ese nombre
    private void createCollectionIfNotExists(String name) {

        if (!collectionRepository.existsByName(name)) {

            Collection collection = new Collection();
            collection.setName(name);

            collectionRepository.save(collection);
        }
    }
}
