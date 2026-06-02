package com.vfdcb.bolao.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vfdcb.bolao.auth.dto.LoginRequest;
import com.vfdcb.bolao.auth.dto.SignupRequest;
import com.vfdcb.bolao.auth.repository.UserRepository;
import com.vfdcb.bolao.config.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }

    @Test
    void testSignupAndLoginFlow() throws Exception {
        // 1. Signup
        SignupRequest signupRequest = new SignupRequest("Integration User", "integration@test.com", "password123");
        
        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Integration User"))
                .andExpect(jsonPath("$.email").value("integration@test.com"))
                .andExpect(cookie().exists("session"))
                .andReturn();

        String sessionCookie = signupResult.getResponse().getCookie("session").getValue();
        assertNotNull(sessionCookie);

        // 2. Login
        LoginRequest loginRequest = new LoginRequest("integration@test.com", "password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("integration@test.com"))
                .andExpect(cookie().exists("session"))
                .andReturn();

        String newSessionCookie = loginResult.getResponse().getCookie("session").getValue();
        
        // 3. Me (Get Current User)
        mockMvc.perform(get("/api/auth/me")
                .cookie(new jakarta.servlet.http.Cookie("session", newSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("integration@test.com"));

        // 4. Logout
        mockMvc.perform(post("/api/auth/logout")
                .cookie(new jakarta.servlet.http.Cookie("session", newSessionCookie)))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("session", 0));
    }

    @Test
    void testSignupDuplicateEmail() throws Exception {
        SignupRequest signupRequest = new SignupRequest("User 1", "duplicate@test.com", "password123");
        
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        SignupRequest duplicateRequest = new SignupRequest("User 2", "duplicate@test.com", "password456");

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already exists"));
    }

    @Test
    void testLoginInvalidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nonexistent@test.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void testMeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
