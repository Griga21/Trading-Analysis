package com.trading.dto;

import com.trading.model.Security;

public record SecurityResponse(
        String ticker,
        String shortName,
        String board,
        boolean active) {

    public static SecurityResponse from(Security security) {
        return new SecurityResponse(
                security.getSecurityId(),
                security.getShortName(),
                security.getBoard(),
                security.isActive());
    }
}
