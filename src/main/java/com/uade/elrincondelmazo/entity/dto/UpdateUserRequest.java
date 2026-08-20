package com.uade.elrincondelmazo.entity.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {

    private String username;
    private String firstName;
    private String lastName;

}
