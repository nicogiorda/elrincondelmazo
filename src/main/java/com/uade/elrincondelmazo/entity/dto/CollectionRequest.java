package com.uade.elrincondelmazo.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CollectionRequest {

    @NotBlank(message = "El nombre de la colección es obligatorio")
    private String name;

    private String description;

}
