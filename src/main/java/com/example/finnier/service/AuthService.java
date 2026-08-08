package com.example.finnier.service;

import com.example.finnier.dto.AuthResponse;
import com.example.finnier.dto.LoginRequest;
import com.example.finnier.dto.RegistrationRequest;
import com.example.finnier.entity.User;
import com.example.finnier.exception.DuplicateEmailException;
import com.example.finnier.exception.InvalidCredentialsException;
import com.example.finnier.exception.TokenValidationException;
import com.example.finnier.repository.UserRepository;
import com.example.finnier.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserService userService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userService.getUserEntityByEmail(request.email());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse register(RegistrationRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new InvalidCredentialsException("Passwords do not match");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(User.RoleType.USER)
                .status(User.UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refreshToken(String refreshToken) {
        Claims claims = jwtService.validateToken(refreshToken);

        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new TokenValidationException("Provided token is not a refresh token");
        }

        String email = claims.getSubject();
        User user = userService.getUserEntityByEmail(email);

        String newAccessToken = jwtService.generateAccessToken(user);

        return new AuthResponse(newAccessToken, refreshToken);
    }
}
