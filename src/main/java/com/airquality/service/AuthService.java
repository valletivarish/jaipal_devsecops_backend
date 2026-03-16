package com.airquality.service;

import com.airquality.dto.request.LoginRequest;
import com.airquality.dto.request.RegisterRequest;
import com.airquality.dto.response.TokenResponse;
import com.airquality.dto.response.UserResponse;
import com.airquality.exception.DuplicateResourceException;
import com.airquality.exception.ForbiddenException;
import com.airquality.exception.UnauthorizedException;
import com.airquality.model.User;
import com.airquality.repository.UserRepository;
import com.airquality.security.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setHashedPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        String token = jwtUtils.generateToken(savedUser.getEmail());

        return TokenResponse.builder()
                .accessToken(token)
                .tokenType("bearer")
                .user(convertToUserResponse(savedUser))
                .build();
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getHashedPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.getIsActive()) {
            throw new ForbiddenException("Account is deactivated");
        }

        String token = jwtUtils.generateToken(user.getEmail());

        return TokenResponse.builder()
                .accessToken(token)
                .tokenType("bearer")
                .user(convertToUserResponse(user))
                .build();
    }

    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
