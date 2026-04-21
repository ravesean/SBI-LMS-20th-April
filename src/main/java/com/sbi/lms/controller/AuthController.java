package com.sbi.lms.controller;

import com.sbi.lms.dto.LoginRequest;
import com.sbi.lms.dto.LoginResponse;
import com.sbi.lms.model.AppUser;
import com.sbi.lms.repository.AppUserRepository;
import com.sbi.lms.security.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoint.
 *
 * POST /api/v1/auth/login
 *   Body: { "email": "admin@sbi.com", "password": "Admin@123" }
 *   Returns: { "token": "<JWT>" }
 *
 * Use the returned token as: Authorization: Bearer <token>
 * Or paste it into the Swagger UI Authorize button.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final JwtUtils          jwtUtils;

    public AuthController(AppUserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtils jwtUtils) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils        = jwtUtils;
    }


    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT token")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        AppUser user = userRepository.findByEmail(req.getEmail())
                .filter(u -> passwordEncoder.matches(req.getPassword(), u.getPasswordHash()))
                .orElse(null);

        if (user == null) {
            // DevSecOps: generic message — don't reveal whether email or password was wrong
            return ResponseEntity.status(401).body("Invalid credentials");
        }
        return ResponseEntity.ok(new LoginResponse(jwtUtils.generateToken(user.getEmail(), user.getRole())));
    }
}
