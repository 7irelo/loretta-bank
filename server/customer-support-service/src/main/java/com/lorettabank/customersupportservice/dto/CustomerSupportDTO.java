package com.lorettabank.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSupportDTO {
    private int id;
    private int userId;
    private String query;
    private String response;
    private String status;
    private String createdAt;
    private String updatedAt;
    private UserDTO user;
}
