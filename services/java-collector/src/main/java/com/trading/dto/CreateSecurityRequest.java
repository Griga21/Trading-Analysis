package com.trading.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSecurityRequest(
        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = "[A-Za-z0-9.-]+")
        String ticker) {
}
