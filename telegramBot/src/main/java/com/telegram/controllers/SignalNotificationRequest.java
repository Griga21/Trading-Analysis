package com.telegram.controllers;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record SignalNotificationRequest(
        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9.-]{1,20}")
        String ticker,
        @NotBlank
        String asOf,
        @Positive
        double close,
        @Positive
        int horizon,
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        double probabilityUp,
        @DecimalMin("0.5")
        @DecimalMax("1.0")
        double threshold
) {
}
