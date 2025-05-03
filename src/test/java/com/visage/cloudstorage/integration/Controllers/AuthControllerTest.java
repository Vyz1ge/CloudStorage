package com.visage.cloudstorage.integration.Controllers;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.visage.cloudstorage.Configurations.SecurityConfig;
import com.visage.cloudstorage.Model.AuthReqest;
import com.visage.cloudstorage.Model.RegisterReqest;
import com.visage.cloudstorage.Model.Role;
import com.visage.cloudstorage.Model.User;
import com.visage.cloudstorage.Repositories.UserRepository;
import com.visage.cloudstorage.Services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @Autowired
    private MockMvc mockMvc;


    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp(){
        userRepository.deleteAll();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.password", postgres::getUsername);
        registry.add("spring.datasource.username", postgres::getPassword);
    }

    @Test
    void shouldBeRegisterAndCorrectSaveUserInDB() throws Exception {
        RegisterReqest registerReqest = RegisterReqest.builder()
                .password("Password")
                .username("Username")
                .build();
        MvcResult result = mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(registerReqest)))
                .andExpect(status().isCreated()) // Ожидаем 201 Created
                .andExpect(jsonPath("$.username").value("Username")).andReturn();
        Optional<User> userOptional = userRepository.findByUsername("Username");
        assertTrue(userOptional.isPresent());
        User user = userOptional.get();
        assertEquals("Username", user.getUsername());
        assertNotEquals("Password", user.getPassword());
    }

    @Test
    void shouldBeRegisterAndCorrectSaveUserInDBAndNotCorrectUsername() throws Exception {
        User userToLogin = User.builder()
                .username("Min")
                .password(passwordEncoder.encode("Password"))
                .role(Role.USER)
                .build();
        userRepository.save(userToLogin);

        AuthReqest authReqest = new AuthReqest();
        authReqest.setUsername("Min");
        authReqest.setPassword("Password");

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(authReqest)))
                .andExpect(status().isBadRequest());
        Optional<User> userOptional = userRepository.findByUsername("Min");
        assertTrue(userOptional.isPresent());
    }

    @Test
    void shouldBeLoginAndCorrectUsernameAndPassword() throws Exception {
        User userToLogin = User.builder()
                .username("Username")
                .password(passwordEncoder.encode("Password"))
                .role(Role.USER)
                .build();
        userRepository.save(userToLogin);

        AuthReqest authReqest = new AuthReqest();
        authReqest.setUsername("Username");
        authReqest.setPassword("Password");

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(authReqest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Username"));
        Optional<User> userOptional = userRepository.findByUsername("Username");
        assertTrue(userOptional.isPresent());
        User user = userOptional.get();
        assertEquals("Username", user.getUsername());
        assertNotEquals("Password", user.getPassword());
    }

    @Test
    void shouldBeLoginAndCorrectLoginAndNotCorrectPassword() throws Exception {
        User userToLogin = User.builder()
                .username("Username")
                .password("Some password")
                .role(Role.USER)
                .build();
        userRepository.save(userToLogin);

        AuthReqest authReqest = new AuthReqest();
        authReqest.setUsername("Username");
        authReqest.setPassword("Password");

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(authReqest)))
                .andExpect(status().isUnauthorized());
        Optional<User> userOptional = userRepository.findByUsername("Username");
        assertTrue(userOptional.isPresent());
    }

    @Test
    void shouldBeLoginAndNotCorrectLoginAndCorrectPassword() throws Exception {
        User userToLogin = User.builder()
                .username("Some username")
                .password(passwordEncoder.encode("Password"))
                .role(Role.USER)
                .build();
        userRepository.save(userToLogin);

        AuthReqest authReqest = new AuthReqest();
        authReqest.setUsername("Username");
        authReqest.setPassword("Password");

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(authReqest)))
                .andExpect(status().isUnauthorized());
        Optional<User> userOptional = userRepository.findByUsername("Some username");
        assertTrue(userOptional.isPresent());
    }
    @Test
    void shouldBeLoginAndNotCorrectAuth() throws Exception {
        User userToLogin = User.builder()
                .username("Min")
                .password(passwordEncoder.encode("Password"))
                .role(Role.USER)
                .build();
        userRepository.save(userToLogin);

        AuthReqest authReqest = new AuthReqest();
        authReqest.setUsername("Min");
        authReqest.setPassword("Password");

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(authReqest)))
                .andExpect(status().isBadRequest());
        Optional<User> userOptional = userRepository.findByUsername("Min");
        assertTrue(userOptional.isPresent());
    }

    @Test
    void shouldBeOutIsCorrect() throws Exception {
        mockMvc.perform(post("/api/auth/sign-out")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(Void.class)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "Username")
    void shouldUserAuthenticatedAndUsernameCorrect() throws Exception {
        User userInDb = new User();
        userInDb.setUsername("Username");
        userInDb.setPassword(passwordEncoder.encode("Password"));
        userRepository.save(userInDb);

        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Username"));
    }
}
