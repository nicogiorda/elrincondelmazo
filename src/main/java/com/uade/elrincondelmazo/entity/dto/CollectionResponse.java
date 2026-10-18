package com.uade.elrincondelmazo.entity.dto;

import com.uade.elrincondelmazo.entity.Collection;

import lombok.Data;

@Data
public class CollectionResponse {

    /*
     * DTO usado para devolver información de una colección a través de la API.
     * Contiene los campos básicos y un helper estático para convertir desde
     * la entidad `Collection` a este DTO.
     */

    private Long id;
    private String name;
    private String description;

    public static CollectionResponse fromCollection(Collection collection) {
        // Convierte la entidad Collection a CollectionResponse (mapeo manual)
        CollectionResponse response = new CollectionResponse();
        response.setId(collection.getId());
        response.setName(collection.getName());
        response.setDescription(collection.getDescription());

        return response;

    }
}
