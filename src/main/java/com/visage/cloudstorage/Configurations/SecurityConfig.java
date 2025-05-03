package com.visage.cloudstorage.Configurations;


import com.visage.cloudstorage.Services.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserService userService;


    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(request -> {
                     CorsConfiguration configuration = new CorsConfiguration();
                     configuration.addAllowedOriginPattern("*");
                     configuration.addAllowedMethod("*");
                     configuration.addAllowedHeader("*");
                     configuration.setAllowCredentials(true);
                     return configuration;
                 }))
//                .cors(cors -> cors.configurationSource(request -> {
//                    CorsConfiguration configuration = new CorsConfiguration();
//                    // ЯВНО указываем разрешенный источник (ваш фронтенд)
//                    configuration.setAllowedOrigins(Arrays.asList("http://localhost:8081")); // Используйте List.of(...) для Java 9+
//                    // Разрешаем нужные методы
//                    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
//                    // Разрешаем необходимые заголовки (Content-Type обязателен, Authorization может понадобиться)
//                    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "X-Requested-With", "Accept", "Origin"));
//                    // !!! РАЗРЕШАЕМ ПЕРЕДАЧУ CREDENTIALS !!!
//                    configuration.setAllowCredentials(true);
//                    // Можно добавить maxAge для кэширования preflight запросов
//                    // configuration.setMaxAge(3600L);
//                    return configuration;
//                }))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
//                                "/api/user/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                .logout(logout -> logout
                        .logoutUrl("/api/auth/sign-out")
                        .logoutSuccessHandler(((request, response, authentication) -> response
                                .setStatus(HttpServletResponse.SC_NO_CONTENT)))
                )





//                СЕССИЯ ВЫСТРОЕНА ТАК ЕСЛИ ПРОВЕРЯЮ НА JWT ТОКЕН
//                .sessionManagement(session -> session
//                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                )
//                .authenticationProvider(authProvider)
//                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(daoAuthenticationProvider());
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(getPasswordEncoder());
        provider.setUserDetailsService(userService.getUsername());
        return provider;
    }

    @Bean
    public PasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

