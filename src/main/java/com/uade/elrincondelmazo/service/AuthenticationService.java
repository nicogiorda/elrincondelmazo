package com.uade.elrincondelmazo.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.uade.elrincondelmazo.controllers.auth.AuthenticationRequest;
import com.uade.elrincondelmazo.controllers.auth.AuthenticationResponse;
import com.uade.elrincondelmazo.controllers.auth.RegisterRequest;
import com.uade.elrincondelmazo.controllers.config.JwtService;
import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.enums.Role;
import com.uade.elrincondelmazo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
        private final UserRepository repository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;

        public AuthenticationResponse register(RegisterRequest request) {

                String email = request.getEmail().trim().toLowerCase();

                if (repository.existsByEmail(email)) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario registrado con este email"
                        );
                }

                User user = User.builder()
                                .firstName(request.getFirstname())
                                .lastName(request.getLastname())
                                .email(email)
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .role(Role.USUARIO)
                                .createdAt(LocalDateTime.now())
                                .active(true)
                                .build();

                repository.save(user);

                String jwtToken = jwtService.generateToken(user);

                return AuthenticationResponse.builder()
                                .accessToken(jwtToken)
                                .build();
        }

        public AuthenticationResponse authenticate(AuthenticationRequest request) {

            String email = request.getEmail().trim().toLowerCase();

            authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                email,
                                                request.getPassword()));

                User user = repository.findByEmail(request.getEmail())
                                .orElseThrow();

                String jwtToken = jwtService.generateToken(user);

                return AuthenticationResponse.builder()
                                .accessToken(jwtToken)
                                .build();
        }
}
