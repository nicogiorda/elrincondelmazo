package com.uade.elrincondelmazo.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.entity.dto.UpdateUserRequest;
import com.uade.elrincondelmazo.exception.EmailAlreadyExistsException;
import com.uade.elrincondelmazo.exception.UserNotFoundException;
import com.uade.elrincondelmazo.repository.UserRepository;
import com.uade.elrincondelmazo.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    /*
     * public UserServiceImpl(UserRepository userRepository) {
     * this.userRepository = userRepository;
     * }
     */

    @Override
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    @Override
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    @Override
    public User updateUser(String email, UpdateUserRequest request) {

        User user = getByEmail(email);

        String newEmail = request.getEmail()
                .trim()
                .toLowerCase();

        if (!user.getEmail().equals(newEmail)
                && userRepository.existsByEmail(newEmail)) {

            throw new EmailAlreadyExistsException(
                    "El email ya está registrado por otro usuario");
        }

        user.setEmail(newEmail);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        return userRepository.save(user);
    }

    @Override
    public void deleteUser(String email) {

        User user = getByEmail(email);

        userRepository.delete(user);
    }

}
