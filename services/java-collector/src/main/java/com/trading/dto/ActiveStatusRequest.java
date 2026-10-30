package com.trading.dto;

import jakarta.validation.constraints.NotNull;

public record ActiveStatusRequest(@NotNull Boolean active) {
}
