package com.finventory.service;

import com.finventory.dto.UserDto;
import com.finventory.model.Role;
import com.finventory.model.User;
import com.finventory.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(this::mapToDto).toList();
    }

    public UserDto createUser(UserDto dto) {
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        var user =
                User.builder()
                        .username(dto.getUsername())
                        .email(dto.getEmail())
                        .role(Role.valueOf(dto.getRole().toUpperCase()))
                        .password(passwordEncoder.encode(dto.getPassword()))
                        .build();
        userRepository.save(user);
        return mapToDto(user);
    }

    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }

    public UserDto getUserByUsername(String username) {
        return userRepository
                .findByUsername(username)
                .map(this::mapToDto)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public void updatePassword(String username, com.finventory.dto.ChangePasswordRequest request) {
        var user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid current password");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
