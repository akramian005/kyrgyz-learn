package com.example.kyrgyzlearn.service;

import com.example.kyrgyzlearn.dto.auth.AuthResponse;
import com.example.kyrgyzlearn.dto.auth.LoginRequest;
import com.example.kyrgyzlearn.dto.auth.RegisterRequest;
import com.example.kyrgyzlearn.jwt.JwtService;
import com.example.kyrgyzlearn.security.CustomUserDetails;
import com.example.kyrgyzlearn.entity.Role;
import com.example.kyrgyzlearn.entity.User;
import com.example.kyrgyzlearn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use: " + email);
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + email));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
