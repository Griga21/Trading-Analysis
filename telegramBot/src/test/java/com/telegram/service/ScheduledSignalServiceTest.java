package com.telegram.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduledSignalServiceTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ScheduledSignalService scheduledSignalService;

    @Test
    void sendsTestMessageWhenScheduledMethodRuns() {
        scheduledSignalService.sendScheduledTestMessage();

        verify(notificationService).sendNotification(
                "Планировщик работает. Алгоритм торговых сигналов пока не подключен.");
    }
}
