package com.uade.elrincondelmazo.service;

import java.util.Optional;

import com.uade.elrincondelmazo.dto.UpdateUserRequest;
import com.uade.elrincondelmazo.entity.User;

public interface UserService {

    User getById(Long id);

    User getByUsername(String username);

    User getByEmail(String email);

    User updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);
}
