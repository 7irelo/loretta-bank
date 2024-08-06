package com.lorettabank.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private String id;
    private String firstName;
    private String lastName;
    private String address;
    private LocalDate dateOfBirth;
    private String occupation;
    private String phone;
    private String email;
    private String username;
    private String password;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
