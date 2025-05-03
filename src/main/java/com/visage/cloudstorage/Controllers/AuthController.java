package com.visage.cloudstorage.Controllers;


import com.visage.cloudstorage.DTO.UserResponse;
import com.visage.cloudstorage.Model.AuthReqest;
import com.visage.cloudstorage.Model.RegisterReqest;
import com.visage.cloudstorage.Model.User;
import com.visage.cloudstorage.Services.AuthService;
import jakarta.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/auth/sign-up")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterReqest reqest) {
        if (reqest.getUsername().length() < 5){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(reqest));
    }

    @PostMapping("/auth/sign-in")
    public ResponseEntity<UserResponse> login(@RequestBody AuthReqest reqest){
        if (reqest.getUsername().length() < 5){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(service.auth(reqest));
    }
    @PostMapping("/auth/sign-out")
    public ResponseEntity<Void> unregister(@AuthenticationPrincipal User user){
        if (user == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    @GetMapping("/user/me")
    public ResponseEntity<UserResponse> pingSession(@AuthenticationPrincipal UserDetails userDetails){
        UserResponse userResponse = new UserResponse(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.OK).body(userResponse);
    }
}
