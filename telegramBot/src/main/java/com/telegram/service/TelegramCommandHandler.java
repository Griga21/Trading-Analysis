package com.telegram.service;

import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class TelegramCommandHandler {

    public String handle(String message) {
        if (message == null || message.isBlank()) {
            return "Отправь /help, чтобы посмотреть доступные команды.";
        }

        String[] parts = message.trim().split("\\s+", 2);
        String command = parts[0].split("@", 2)[0].toLowerCase(Locale.ROOT);
        String argument = parts.length == 2 ? parts[1].trim() : "";

        return switch (command) {
            case "/start" -> "Привет! Я бот для отслеживания акций. Отправь /help, чтобы узнать команды.";
            case "/help" -> """
                    Доступные команды:
                    /addNew <тикер> — добавить акцию в отслеживание
                    Например: /addNew SBER
                    """;
            case "/addnew" -> handleAddNew(argument);
            default -> command.startsWith("/")
                    ? "Неизвестная команда. Отправь /help, чтобы посмотреть доступные команды."
                    : "Пока я понимаю только команды. Отправь /help, чтобы посмотреть список.";
        };
    }

    private String handleAddNew(String ticker) {
        if (ticker.isBlank()) {
            return "Укажи тикер акции. Например: /addNew SBER";
        }

        String normalizedTicker = ticker.toUpperCase(Locale.ROOT);
        if (!normalizedTicker.matches("[A-Z0-9.-]{1,20}")) {
            return "Тикер должен содержать только латинские буквы, цифры, точку или дефис.";
        }

        return "Понял: ты хочешь отслеживать " + normalizedTicker
                + ". Распознавание команды работает, но добавление акции ещё не подключено.";
    }
}
