package com.uade.elrincondelmazo.service;

import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.entity.dto.UpdateUserRequest;

public interface UserService {

    User getById(Long id);

    User getByEmail(String email);

    User updateUser(String email, UpdateUserRequest request);

    void deleteUser(String email);

    User promoteToAdmin(Long id);
}
