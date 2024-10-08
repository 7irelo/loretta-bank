package com.lorettabank.userservice.service;

import com.lorettabank.userservice.dto.UserDTO;
import com.lorettabank.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public UserDTO registerUser(UserDTO userDTO) {
        userDTO.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        userRepository.save(userDTO); // Save or update user
        return userDTO;
    }

    public Optional<UserDTO> loginUser(String username, String password) {
        Optional<UserDTO> userDTO = userRepository.findByUsername(username);
        if (userDTO.isPresent() && passwordEncoder.matches(password, userDTO.get().getPassword())) {
            return userDTO;
        }
        return Optional.empty();
    }

    public Optional<UserDTO> getCurrentUser(String id) {
        // Fetch user with details including accounts, transactions, etc.
        return userRepository.findByIdWithDetails(id);
    }

    public UserDTO updateUser(UserDTO userDTO) {
        // Note: Update logic here is essentially the same as save
        userRepository.save(userDTO);
        return userDTO;
    }
}
