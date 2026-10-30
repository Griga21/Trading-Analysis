package com.telegram.controllers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.telegram.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class TelegramControllerTest {

    @Mock
    private NotificationService notificationService;

    private TelegramController controller;

    @BeforeEach
    void setUp() {
        controller = new TelegramController(notificationService);
        ReflectionTestUtils.setField(controller, "analyticsApiToken", "shared-test-token");
    }

    @Test
    void sendsAuthenticatedSignalToTelegram() {
        SignalNotificationRequest signal = new SignalNotificationRequest(
                "SBER",
                "2026-10-30T00:00:00",
                250.5,
                15,
                0.72,
                0.60
        );

        controller.receiveSignal("shared-test-token", signal);

        verify(notificationService).sendNotification(
                org.mockito.ArgumentMatchers.argThat(message ->
                        message.contains("Сигнал: SBER")
                                && message.contains("72.0%"))
        );
    }

    @Test
    void rejectsUnauthenticatedSignal() {
        SignalNotificationRequest signal = new SignalNotificationRequest(
                "SBER",
                "2026-10-30T00:00:00",
                250.5,
                15,
                0.72,
                0.60
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.receiveSignal("wrong-token", signal)
        );

        org.junit.jupiter.api.Assertions.assertEquals(401, error.getStatusCode().value());
    }
}
