package com.uade.elrincondelmazo.controllers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.uade.elrincondelmazo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {
    private final UserRepository repository;

    @Bean public UserDetailsService userDetailsService() {
        return email -> repository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    @Bean public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService());
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;

        //Metemos el userDetailsService dentro del Dao ya que en esta versión de Spring Security
        //no es necesario hacerlo explícitamente.
    }

    @Bean public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
