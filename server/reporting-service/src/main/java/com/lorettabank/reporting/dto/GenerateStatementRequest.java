package com.lorettabank.reporting.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateStatementRequest {

    @NotNull(message = "Account ID is required")
    private Long accountId;

    @NotNull(message = "periodFrom is required")
    private LocalDateTime periodFrom;

    @NotNull(message = "periodTo is required")
    private LocalDateTime periodTo;
}
