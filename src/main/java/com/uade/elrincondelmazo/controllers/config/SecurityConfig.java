package com.uade.elrincondelmazo.controllers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final AuthenticationProvider authenticationProvider;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)

                                .authorizeHttpRequests(req -> req
                                                .requestMatchers("/api/v1/auth/**").permitAll()
                                                .requestMatchers("/error/**").permitAll()

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/products",
                                                                "/products/**")
                                                .permitAll()

                                                .requestMatchers(HttpMethod.POST, "/collections/**")
                                                .hasAuthority("ADMIN")

                                                .requestMatchers(HttpMethod.PUT, "/collections/**")
                                                .hasAuthority("ADMIN")

                                                .requestMatchers(HttpMethod.DELETE, "/collections/**")
                                                .hasAuthority("ADMIN")

                                                .anyRequest().authenticated())

                                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))

                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint((request, response, authException) -> response
                                                                .sendError(
                                                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                                                "Unauthorized")) //401 Unauthorized
                                                .accessDeniedHandler((request, response, accessDeniedException) -> response
                                                                .sendError(
                                                                                HttpServletResponse.SC_FORBIDDEN,
                                                                                "Forbidden"))) //403 Forbidden

                                .authenticationProvider(authenticationProvider)

                                .addFilterBefore(
                                                jwtAuthFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
