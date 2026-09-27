package com.example.messenger.service;

import com.example.messenger.dto.LoginRequest;
import com.example.messenger.dto.RegisterRequest;
import com.example.messenger.dto.TokenResponse;
import com.example.messenger.entity.UserAccount;
import com.example.messenger.repository.UserRepository;
import com.example.messenger.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already registered");
        }
        UserAccount user = userRepository.save(
                new UserAccount(request.username(), passwordEncoder.encode(request.password())));
        return tokenResponse(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        UserAccount user = userRepository.findByUsername(request.username())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        return tokenResponse(user);
    }

    private TokenResponse tokenResponse(UserAccount user) {
        return new TokenResponse(user.getId(), user.getUsername(),
                jwtService.createToken(user.getId(), user.getUsername()), "Bearer");
    }
}