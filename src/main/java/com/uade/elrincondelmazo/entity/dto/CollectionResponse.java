package com.uade.elrincondelmazo.entity.dto;

import com.uade.elrincondelmazo.entity.Collection;

import lombok.Data;

@Data
public class CollectionResponse {

    private Long id;
    private String name;
    private String description;

    public static CollectionResponse fromCollection(Collection collection) {
        CollectionResponse response = new CollectionResponse();
        response.setId(collection.getId());
        response.setName(collection.getName());
        response.setDescription(collection.getDescription());

        return response;

    }
}
