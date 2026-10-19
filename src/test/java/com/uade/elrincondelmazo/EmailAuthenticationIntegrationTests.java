package com.uade.elrincondelmazo;

import com.uade.elrincondelmazo.repository.UserRepository;
import com.uade.elrincondelmazo.enums.Role;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("audit")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class EmailAuthenticationIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Value("${application.security.jwt.secretKey}") String secret;
    String email, body, token;

    @BeforeEach void registerOriginalContract() throws Exception {
        email = "qa_" + UUID.randomUUID() + "@example.invalid";
        body = "{\"firstname\":\"QA\",\"lastname\":\"Email\",\"email\":\"%s\",\"password\":\"Password123!\",\"role\":\"ADMIN\"}".formatted(email);
        var result = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.access_token").isString()).andReturn();
        token = JsonPath.read(result.getResponse().getContentAsString(), "$.access_token");
    }
    @Test void registrationKeepsEmailIdentityBcryptAndUserRole() throws Exception {
        var user = users.findByEmail(email).orElseThrow();
        assertThat(user.getUsername()).isEqualTo(email);
        assertThat(user.getRole()).isEqualTo(Role.USUARIO);
        assertThat(encoder.matches("Password123!", user.getPasswordHash())).isTrue();
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value("QA")).andExpect(jsonPath("$.username").doesNotExist());
    }
    @Test void loginAndPreviouslyIssuedEmailJwtKeepWorking() throws Exception {
        mvc.perform(post("/api/v1/auth/authenticate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"Password123!\"}".formatted(email)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.access_token").isString());
        String existing = Jwts.builder().subject(email).issuedAt(new Date(System.currentTimeMillis()-60000))
                .expiration(new Date(System.currentTimeMillis()+60000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))).compact();
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + existing))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
    }
    @Test void duplicateEmailIsRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }
    @Test void invalidTokenAndCredentialsCannotAuthenticate() throws Exception {
        mvc.perform(get("/users/me").header("Authorization", "Bearer invalid-token")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/authenticate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"incorrect\"}".formatted(email)))
                .andExpect(status().is4xxClientError());
    }
}
