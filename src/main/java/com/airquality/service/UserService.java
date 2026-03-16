package com.airquality.service;

import com.airquality.dto.request.UserUpdateRequest;
import com.airquality.dto.response.UserResponse;
import com.airquality.exception.ForbiddenException;
import com.airquality.exception.ResourceNotFoundException;
import com.airquality.model.User;
import com.airquality.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return convertToUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request, Long currentUserId) {
        if (!id.equals(currentUserId)) {
            throw new ForbiddenException("You can only update your own profile");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }

        User savedUser = userRepository.save(user);
        return convertToUserResponse(savedUser);
    }

    public List<UserResponse> getAllUsers(int skip, int limit) {
        int page = limit > 0 ? skip / limit : 0;
        Pageable pageable = PageRequest.of(page, limit);
        return userRepository.findAll(pageable).getContent().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
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
