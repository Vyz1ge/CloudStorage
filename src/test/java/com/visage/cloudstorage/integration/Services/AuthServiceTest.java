package com.visage.cloudstorage.integration.Services;


import com.visage.cloudstorage.DTO.UserResponse;
import com.visage.cloudstorage.Model.AuthReqest;
import com.visage.cloudstorage.Model.RegisterReqest;
import com.visage.cloudstorage.Model.Role;
import com.visage.cloudstorage.Model.User;
import com.visage.cloudstorage.Repositories.UserRepository;
import com.visage.cloudstorage.Services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.Assert.*;

@Testcontainers
@SpringBootTest
public class AuthServiceTest {
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.password", postgres::getUsername);
        registry.add("spring.datasource.username", postgres::getPassword);
    }


    @Test
    public void shouldBeRegister() {
        // Arrange
        RegisterReqest registerReqest = RegisterReqest.builder()
                .username("username")
                .password("password")
                .build();
        // Act
        UserResponse userResponse = authService.register(registerReqest);

        // Asserts
        assertNotNull(userResponse.username(),"Юзернейм не нулл");
        Optional<User> optionalExpectedUser = userRepository.findByUsername(registerReqest.getUsername());
        assertTrue(optionalExpectedUser.isPresent());

        User expectedUser = optionalExpectedUser.get();
        assertEquals(expectedUser.getUsername(), userResponse.username());
        assertEquals(Role.USER, expectedUser.getRole());
        assertNotEquals(registerReqest.getPassword(), expectedUser.getPassword());
        assertTrue(passwordEncoder.matches(registerReqest.getPassword(), expectedUser.getPassword()));
    }

    @Test
    void shouldBeLoginAndNotRegister(){
        // Arrange
        AuthReqest authReqest = new AuthReqest();
        authReqest.setUsername("Some user");
        authReqest.setPassword("Some pass");
        // act
        assertThrows(BadCredentialsException.class, () -> authService.auth(authReqest));
    }

    @Test
    void shouldBeLoginAndNotAuthRequest(){
        assertThrows(RuntimeException.class , () -> authService.auth(null));
    }

    @Test
    void shouldBeLoginAndRegister() {
        RegisterReqest register = RegisterReqest.builder()
                .username("username")
                .password("password")
                .build();
        // Act
        UserResponse userResponse = authService.register(register);
        AuthReqest authReqest = new AuthReqest();
        authReqest.setUsername("username");
        authReqest.setPassword("password");

        // act
        assertNotNull(userResponse.username(),"Юзернейм не нулл");
        Optional<User> optionalExpectedUser = userRepository.findByUsername(userResponse.username());
        assertTrue(optionalExpectedUser.isPresent());

        User expectedUser = optionalExpectedUser.get();
        assertEquals(expectedUser.getUsername(), userResponse.username());
        assertEquals(Role.USER, expectedUser.getRole());
        assertNotEquals(userResponse.username(), expectedUser.getPassword());
    }


    @Test
    void shouldNotRegisterUserWithExistingUsername() {
        RegisterReqest registerReqest = RegisterReqest.builder()
                .username("duplicateUser")
                .password("password1")
                .build();
        authService.register(registerReqest);

        RegisterReqest duplicateReqest = RegisterReqest.builder()
                .username("duplicateUser")
                .password("password2")
                .build();
        assertThrows(Exception.class, () -> authService.register(duplicateReqest));
    }



}
