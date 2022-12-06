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
        UserDTO userDTO = UserDTO.fromRequest(userRequest);
        UserDTO registeredUser = userService.registerUser(userDTO);
        return ResponseEntity.status(201).body(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody UserRequest userRequest) {
        Optional<UserDTO> userDTO = userService.loginUser(userRequest.getUsername(), userRequest.getPassword());
        if (userDTO.isPresent()) {
            String token = "some-generated-token";
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
        UserDTO userDTO = UserDTO.fromRequest(userRequest);
        userDTO.setId(id);
        UserDTO updatedUser = userService.updateUser(userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> patchUser(@PathVariable String id, @RequestBody UserRequest userRequest) {
        UserDTO userDTO = UserDTO.fromRequest(userRequest);
        userDTO.setId(id);
        UserDTO updatedUser = userService.updateUser(userDTO);
        return ResponseEntity.ok(updatedUser);
    }
}
