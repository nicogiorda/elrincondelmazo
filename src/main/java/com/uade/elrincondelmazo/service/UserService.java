package com.uade.elrincondelmazo.service;

import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.entity.dto.UpdateUserRequest;

public interface UserService {

    User getById(Long id);

    User getByUsername(String username);

    User getByEmail(String email);

    User updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);
}
