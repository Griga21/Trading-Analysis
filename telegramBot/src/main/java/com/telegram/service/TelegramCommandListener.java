package com.telegram.service;

import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

import ch.qos.logback.core.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramCommandListener {

    private final NotificationService notifier;
    private long offset = 0;

    @Scheduled(fixedDelay = 2000)
    public void poll() {
        WebClient webClient = WebClient.create("https://api.telegram.org");
        try {
            JsonNode resp = webClient.get()
                    .uri("/bot{t}/getUpdates?offset={o}&timeout=0", "token",
                            offset)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (resp == null || !resp.has("result"))
                return;

            for (JsonNode update : resp.get("result")) {
                offset = update.get("update_id").asLong() + 1;

                JsonNode msg = update.get("message");
                if (msg == null || !msg.has("text"))
                    continue;

                String chatId = msg.get("chat").get("id").asText();
                String text = msg.get("text").asText();

                notifier.sendNotification("Я получил: " + text);
            }
        } catch (Exception e) {
            log.error("Telegram polling failed: {}", e.getMessage());
        }
    }
}