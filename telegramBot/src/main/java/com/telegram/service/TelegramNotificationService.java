package com.telegram.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class TelegramNotificationService implements NotificationService {

    private final WebClient webClient = WebClient.create("https://api.telegram.org");
    @Value ("${telegram.bot-token}")
    private String botToken;
    @Value ("${telegram.chat-id}")
    private String chatId;


    @Override
    public void sendNotification(String message) {
        sendNotification(chatId, message);
    }

    @Override
    public void sendNotification(String recipientChatId, String message) {
        try {
            webClient.post()
                    .uri("/bot{token}/sendMessage", botToken)
                    .bodyValue(Map.of("chat_id", recipientChatId, "text", message))
                     .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(10));
            log.info("Telegram message sent");
        } catch (Exception e) {
            log.error("Failed to send Telegram message", e);
            throw new IllegalStateException("Telegram message delivery failed", e);
        }
    }
}
