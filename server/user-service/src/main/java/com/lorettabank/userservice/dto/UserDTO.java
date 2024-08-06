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
}
