package com.visage.cloudstorage.integration.Services;


import com.visage.cloudstorage.Model.Role;
import com.visage.cloudstorage.Model.User;
import com.visage.cloudstorage.Repositories.UserRepository;
import com.visage.cloudstorage.Services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
public class UserServiceTest {
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp(){
        userRepository.deleteAll();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url",postgres::getJdbcUrl);
        registry.add("spring.datasource.password",postgres::getUsername);
        registry.add("spring.datasource.username",postgres::getPassword);
    }

    @Test
    public void getUsernameTest(){
        User user = User.builder()
                .username("Username")
                .password("Password")
                .role(Role.USER)
                .build();
        userRepository.save(user);
        UserDetailsService userDetailsService = userService.getUsername();

        UserDetails loadeduserDetails = assertDoesNotThrow(() -> userDetailsService.loadUserByUsername("Username"),"Не выбрасывается");
        assertNotNull(loadeduserDetails.getUsername(),"Загруженный юзер не может быть пустым");
        assertEquals("Username",loadeduserDetails.getUsername());
    }

    @Test
    public void shouldThrowUsernameNotFoundException_WhenUserDoesNotExist(){
        UserDetailsService userDetailsService = userService.getUsername();
        assertThrows(UsernameNotFoundException.class,() -> userDetailsService.loadUserByUsername("Username"));
    }


}
