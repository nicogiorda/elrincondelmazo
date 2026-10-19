package com.uade.elrincondelmazo.controllers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final AuthenticationProvider authenticationProvider;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http


                                .cors(Customizer.withDefaults())
                                .csrf(AbstractHttpConfigurer::disable)

                                .authorizeHttpRequests(req -> req
                                                .requestMatchers("/api/v1/auth/**").permitAll()
                                                .requestMatchers("/error/**").permitAll()

                                                .requestMatchers(HttpMethod.GET, "/products/mine").authenticated()

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/products",
                                                                "/products/**",
                                                                "/collections",
                                                                "/collections/**")
                                                .permitAll()

                                                .requestMatchers(HttpMethod.GET, "/promotions/public").permitAll()
                                                // Toda otra operación de promociones es administrativa.
                                                .requestMatchers("/promotions", "/promotions/**").hasAuthority("ADMIN")

                                                .requestMatchers(HttpMethod.POST, "/collections/**")
                                                .hasAuthority("ADMIN")

                                                .requestMatchers(HttpMethod.PUT, "/collections/**")
                                                .hasAuthority("ADMIN")

                                                .requestMatchers(HttpMethod.DELETE, "/collections/**")
                                                .hasAuthority("ADMIN")

                                                .requestMatchers(HttpMethod.PATCH, "/users/promote/*")
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

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOrigins(List.of("http://localhost:5173"));
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setExposedHeaders(List.of("Authorization"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                return source;
        }

}
