package com.uade.elrincondelmazo.service;

import org.springframework.stereotype.Service;

import com.uade.elrincondelmazo.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    

    
 
    
}