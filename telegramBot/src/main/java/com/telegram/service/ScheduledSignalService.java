package com.telegram.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledSignalService {

    private static final String TEST_MESSAGE =
            "Планировщик работает. Алгоритм торговых сигналов пока не подключен.";

    private final NotificationService notificationService;

    @Scheduled(cron = "${telegram.signal.cron:0 * * * * *}", zone = "${telegram.signal.zone:Europe/Moscow}")
    public void sendScheduledTestMessage() {
        log.info("Sending scheduled signal scheduler test message");
        notificationService.sendNotification(TEST_MESSAGE);
    }
}
