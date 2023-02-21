package com.lorettabank.userservice.controller;

import com.lorettabank.userservice.dto.UserDTO;
import com.lorettabank.userservice.dto.UserRequest;
import com.lorettabank.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRequest userRequest) {
        UserDTO userDTO = new UserDTO();
        userDTO.setFirstName(userRequest.getFirstName());
        userDTO.setLastName(userRequest.getLastName());
        userDTO.setEmail(userRequest.getEmail());
        userDTO.setDateOfBirth(userRequest.getDateOfBirth());
        userDTO.setAddress(userRequest.getAddress());
        userDTO.setOccupation(userRequest.getOccupation());
        userDTO.setPhone(userRequest.getPhone());
        userDTO.setUsername(userRequest.getUsername());
        userDTO.setPassword(userRequest.getPassword());

        UserDTO registeredUser = userService.registerUser(userDTO);
        return ResponseEntity.status(201).body(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody UserRequest userRequest) {
        Optional<UserDTO> userDTO = userService.loginUser(userRequest.getUsername(), userRequest.getPassword());
        if (userDTO.isPresent()) {
            String token = "some-generated-token"; // Replace with actual token generation logic
            return ResponseEntity.ok().header("auth-token", token).body(userDTO.get());
        } else {
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestParam String userId) {
        Optional<UserDTO> userDTO = userService.getCurrentUser(userId);
        if (userDTO.isPresent()) {
            return ResponseEntity.ok(userDTO.get());
        } else {
            return ResponseEntity.status(404).body("User not found");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable String id, @RequestBody UserRequest userRequest) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(id);
        userDTO.setFirstName(userRequest.getFirstName());
        userDTO.setLastName(userRequest.getLastName());
        userDTO.setEmail(userRequest.getEmail());
        userDTO.setDateOfBirth(userRequest.getDateOfBirth());
        userDTO.setAddress(userRequest.getAddress());
        userDTO.setOccupation(userRequest.getOccupation());
        userDTO.setPhone(userRequest.getPhone());
        userDTO.setUsername(userRequest.getUsername());
        userDTO.setPassword(userRequest.getPassword());

        UserDTO updatedUser = userService.updateUser(userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> patchUser(@PathVariable String id, @RequestBody UserRequest userRequest) {
        // Here, we assume partial updates, so only set non-null values from userRequest
        UserDTO existingUser = userService.getCurrentUser(id).orElseThrow(() -> new RuntimeException("User not found"));
        if (userRequest.getFirstName() != null) existingUser.setFirstName(userRequest.getFirstName());
        if (userRequest.getLastName() != null) existingUser.setLastName(userRequest.getLastName());
        if (userRequest.getEmail() != null) existingUser.setEmail(userRequest.getEmail());
        if (userRequest.getDateOfBirth() != null) existingUser.setDateOfBirth(userRequest.getDateOfBirth());
        if (userRequest.getAddress() != null) existingUser.setAddress(userRequest.getAddress());
        if (userRequest.getOccupation() != null) existingUser.setOccupation(userRequest.getOccupation());
        if (userRequest.getPhone() != null) existingUser.setPhone(userRequest.getPhone());
        if (userRequest.getUsername() != null) existingUser.setUsername(userRequest.getUsername());
        if (userRequest.getPassword() != null) existingUser.setPassword(userRequest.getPassword());

        UserDTO updatedUser = userService.updateUser(existingUser);
        return ResponseEntity.ok(updatedUser);
    }
}
