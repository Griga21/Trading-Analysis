package com.telegram.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telegram.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TelegramController {

    private final NotificationService notifier;

    @PostMapping("/api/notify-test")
    public String test() {
        notifier.sendNotification("Привет от Telegram Collector");
        return "ok";
    }
}