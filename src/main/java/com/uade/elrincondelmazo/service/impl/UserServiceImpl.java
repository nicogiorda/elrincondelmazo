package com.uade.elrincondelmazo.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.entity.dto.UpdateUserRequest;
import com.uade.elrincondelmazo.exception.UserNotFoundException;
import com.uade.elrincondelmazo.service.UserService;
import com.uade.elrincondelmazo.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {

    @Autowired 
    private UserRepository userRepository;

    /*public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }*/

    @Override
    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    @Override
    public User getByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
    }

    @Override
    public User getByEmail(String email){
        return userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    @Override
    public User updateUser(Long id, UpdateUserRequest request) {

        User user = getById(id);

        user.setUsername(request.getUsername());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        return userRepository.save(user);
        
    }

    @Override
    public void deleteUser(Long id) {
        User user = getById(id);
        userRepository.delete(user);
    }

    
}
