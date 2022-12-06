package com.lorettabank.userservice.dto;

import lombok.Data;

@Data
public class UserDTO {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String dateOfBirth;
    private String address;
    private String occupation;
    private String phone;
    private String username;
    private String password;

    public static UserDTO fromRequest(UserRequest userRequest) {
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
        return userDTO;
    }
}
