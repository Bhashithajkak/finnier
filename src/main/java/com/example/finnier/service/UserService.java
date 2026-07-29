package com.example.finnier.service;

import com.example.finnier.dto.PasswordChangeDto;
import com.example.finnier.dto.UserRequest;
import com.example.finnier.dto.UserResponse;
import com.example.finnier.entity.User;
import com.example.finnier.exception.InvalidCredentialsException;
import com.example.finnier.exception.PasswordMismatchException;
import com.example.finnier.exception.UserNotFoundException;
import com.example.finnier.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(UserRequest userRequest) {
        User user = userRepository.save(mapToEntity(userRequest));
        return mapToResponseDto(user);
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        return mapToResponseDto(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        return mapToResponseDto(user);
    }

    public List<UserResponse> getAllUsers(){
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    public List<UserResponse> getAllUsersByRole(User.RoleType role) {
        List<User> users = userRepository.findByRole(role);
        return users.stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    public List<UserResponse> getAllUsersByStatus(User.UserStatus status) {
        List<User> users = userRepository.findAll().stream()
                .filter(user -> user.getStatus() == status)
                .toList();
        return users.stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    public UserResponse updateUser(Long userId, UserRequest userRequest) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        existingUser.setFirstName(userRequest.firstName());
        existingUser.setLastName(userRequest.lastName());
        existingUser.setEmail(userRequest.email());
        existingUser.setPassword(userRequest.password());
        existingUser.setRole(userRequest.role());
        existingUser.setStatus(userRequest.status());

        User updatedUser = userRepository.save(existingUser);
        return mapToResponseDto(updatedUser);
    }

    public UserResponse updateUserStatus(Long userId, User.UserStatus status) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        existingUser.setStatus(status);
        User updatedUser = userRepository.save(existingUser);
        return mapToResponseDto(updatedUser);
    }

    public void deactivateUserStatus(Long userId) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        existingUser.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(existingUser);
    }

    public UserResponse updateUserRole(Long userId, User.RoleType role) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        existingUser.setRole(role);
        User updatedUser = userRepository.save(existingUser);
        return mapToResponseDto(updatedUser);
    }

    public UserResponse updateUserPassword(Long userId, PasswordChangeDto requestDto) {
        if(requestDto.currentPassword().equals(requestDto.newPassword())) {
            throw new PasswordMismatchException("New password cannot be the same as the current password");
        }
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        if (!passwordEncoder.matches(requestDto.currentPassword(), existingUser.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        if (!requestDto.newPassword().equals(requestDto.confirmNewPassword())) {
            throw new PasswordMismatchException("New password and confirmation do not match");
        }
        existingUser.setPassword(passwordEncoder.encode(requestDto.newPassword()));
        User updatedUser = userRepository.save(existingUser);
        return mapToResponseDto(updatedUser);
    }

    public void deleteUser(Long userId) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        userRepository.delete(existingUser);
    }


    private User mapToEntity(UserRequest userRequest) {
        return User.builder()
                .firstName(userRequest.firstName())
                .lastName(userRequest.lastName())
                .email(userRequest.email())
                .password(passwordEncoder.encode(userRequest.password()))
                .role(userRequest.role())
                .status(userRequest.status())
                .build();
    }
    private UserResponse mapToResponseDto(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
