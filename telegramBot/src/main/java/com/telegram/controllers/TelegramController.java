package com.telegram.controllers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.telegram.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TelegramController {

    private static final String ANALYTICS_TOKEN_HEADER = "X-Analytics-Token";

    private final NotificationService notifier;

    @Value("${analytics.api-token}")
    private String analyticsApiToken;

    @PostMapping("/api/notify-test")
    public String test(
            @RequestHeader(value = ANALYTICS_TOKEN_HEADER, required = false) String requestToken
    ) {
        if (!isAuthorized(requestToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid analytics token");
        }

        notifier.sendNotification("Привет от Telegram Collector");
        return "ok";
    }

    @PostMapping("/api/signals")
    public String receiveSignal(
            @RequestHeader(value = ANALYTICS_TOKEN_HEADER, required = false) String requestToken,
            @Valid @RequestBody SignalNotificationRequest signal
    ) {
        if (!isAuthorized(requestToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid analytics token");
        }

        notifier.sendNotification(formatSignal(signal));
        return "ok";
    }

    private boolean isAuthorized(String requestToken) {
        if (requestToken == null || analyticsApiToken == null || analyticsApiToken.isBlank()) {
            return false;
        }

        return MessageDigest.isEqual(
                analyticsApiToken.getBytes(StandardCharsets.UTF_8),
                requestToken.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String formatSignal(SignalNotificationRequest signal) {
        return String.format(Locale.ROOT, """
                Сигнал: %s
                Дата свечи: %s
                Цена закрытия: %.2f
                Горизонт: %d торговых свечей
                Вероятность роста: %.1f%% (порог %.0f%%)
                Исследовательский сигнал, не торговая рекомендация.
                """,
                signal.ticker(),
                signal.asOf(),
                signal.close(),
                signal.horizon(),
                signal.probabilityUp() * 100,
                signal.threshold() * 100
        );
    }
}