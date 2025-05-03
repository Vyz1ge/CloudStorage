package com.visage.cloudstorage.Services;

import com.visage.cloudstorage.DTO.UserResponse;
import com.visage.cloudstorage.Model.*;
import com.visage.cloudstorage.Repositories.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@Transactional
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
//    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    private final ResourceService resourceService;

    public AuthService(UserRepository repository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, ResourceService resourceService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.resourceService = resourceService;
    }

    public UserResponse register(RegisterReqest reqest) {
//        System.out.println(reqest.getUsername() + " reqest");
        User user = User.builder()
                .username(reqest.getUsername())
                .password(passwordEncoder.encode(reqest.getPassword()))
                .role(Role.USER)
                .build();

//        if (repository.findByUsername(user.getUsername()).isPresent()){
//            throw new UserAlredyExists("User already exists");
//        }
        repository.save(user);
//        String jwtToken = jwtService.generateToken(user);         могла бы быть проверка на jwt токен

        resourceService.createTheCentralPoint(user.getId());


        Authentication authenticate;
        try {
            authenticate = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            reqest.getUsername(),
                            reqest.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error during authentication", e);
        }
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authenticate);

        HttpSession session;
        try {
            session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest().getSession(true);
        } catch (IllegalStateException e) {
            throw new RuntimeException("Failed to obtain HttpSession", e);
        }

        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

        Object principal = authenticate.getPrincipal();
        if (!(principal instanceof User)) {
            throw new IllegalStateException("Unexpected principal type: " + principal.getClass().getName());
        }
//        System.out.println(user.getUsername() + " user");
        UserResponse userResponse = new UserResponse(user.getUsername());
//        System.out.println(userResponse.username());
        return userResponse;
    }
//
//    public UserResponse auth(AuthReqest reqest) {
//        Authentication authenticate = authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(
//                        reqest.getUsername(),
//                        reqest.getPassword()
//                )
//        );
//        SecurityContext securityContext = SecurityContextHolder.getContext();
//        securityContext.setAuthentication(authenticate);
//
//        HttpSession session = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
//                .getRequest().getSession();
////        String jwtToken = jwtService.generateToken(user);  ТУТ БЫ ПАРСИЛИ jwt token
//
//        session.setAttribute("SPRING_SECURITY_CONTEXT",securityContext);
//
//        User user = (User) authenticate.getPrincipal();
//
//
//
//
//
////        User user = repository.findByEmail(reqest.getEmail()).orElseThrow();
////        return AuthResponse.builder().token(jwtToken).build();
//
//
//        UserResponse userResponse = new UserResponse(user.getUsername());
//
//        return userResponse;
//    }

public UserResponse auth(AuthReqest reqest) {
    Authentication authenticate;
    try {
        authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        reqest.getUsername(),
                        reqest.getPassword()
                )
        );
    } catch (AuthenticationException e) {
        throw e;
    } catch (Exception e) {
        throw new RuntimeException("Unexpected error during authentication", e);
    }

    SecurityContext securityContext = SecurityContextHolder.getContext();
    securityContext.setAuthentication(authenticate);

    HttpSession session;
    try {
        session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest().getSession(true);
    } catch (IllegalStateException e) {
        throw new RuntimeException("Failed to obtain HttpSession", e);
    }

    session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

    Object principal = authenticate.getPrincipal();
    if (!(principal instanceof User)) {
        throw new IllegalStateException("Unexpected principal type: " + principal.getClass().getName());
    }
    User user = (User) principal;

    UserResponse userResponse = new UserResponse(user.getUsername());
    return userResponse;
}
}
