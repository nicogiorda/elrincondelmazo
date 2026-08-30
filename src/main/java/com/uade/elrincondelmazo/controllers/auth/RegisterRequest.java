package com.uade.elrincondelmazo.controllers.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    private String firstname;
    private String lastname;
    private String email;
    private String password;

    /*
     * No usamos atributo role porque si lo dejamos, alguien podría
     * autoplocamarse admin. De esta forma, se controla el rol del usuario
     */
}
