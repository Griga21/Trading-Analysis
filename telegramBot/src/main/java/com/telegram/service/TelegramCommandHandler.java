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
            case "/start" -> startResponse();
            case "/help" -> helpResponse();
            case "/addnew" -> handleAddNew(argument);
            case "/remove" -> handleTickerCommand(argument, "/remove", "убрать из отслеживания");
            case "/status" -> handleTickerCommand(argument, "/status", "показывать статус");
            case "/list" -> handleNoArgumentCommand(argument, "Список отслеживаемых акций");
            case "/signals" -> handleSignals(argument);
            default -> command.startsWith("/")
                    ? "Неизвестная команда. Отправь /help, чтобы посмотреть доступные команды."
                    : "Пока я понимаю только команды. Отправь /help, чтобы посмотреть список.";
        };
    }

    private String startResponse() {
        return "Привет! Я бот для отслеживания акций. Отправь /help, чтобы узнать команды.";
    }

    private String helpResponse() {
        return """
                Доступные команды:
                /addNew <тикер> — добавить акцию в отслеживание
                /remove <тикер> — убрать акцию из отслеживания
                /list — показать отслеживаемые акции
                /status <тикер> — проверить статус акции
                /signals on|off — включить или выключить сигналы
                Пока команды являются заглушками и не изменяют настройки.
                """;
    }

    private String handleAddNew(String ticker) {
        String validationError = validateTicker(ticker, "/addNew");
        if (validationError != null) {
            return validationError;
        }

        String normalizedTicker = ticker.toUpperCase(Locale.ROOT);
        return "Понял: ты хочешь отслеживать " + normalizedTicker
                + ". Добавление пока не подключено.";
    }

    private String handleTickerCommand(String ticker, String command, String action) {
        String validationError = validateTicker(ticker, command);
        if (validationError != null) {
            return validationError;
        }

        String normalizedTicker = ticker.toUpperCase(Locale.ROOT);
        return "Понял: ты хочешь " + action + " " + normalizedTicker
                + ". Функция пока не подключена.";
    }

    private String handleNoArgumentCommand(String argument, String title) {
        if (!argument.isBlank()) {
            return "Команда /list не принимает аргументы.";
        }
        return title + " пока недоступен: функция ещё не подключена.";
    }

    private String handleSignals(String argument) {
        if (!argument.equalsIgnoreCase("on") && !argument.equalsIgnoreCase("off")) {
            return "Укажи режим: /signals on или /signals off.";
        }

        String requestedMode = argument.toLowerCase(Locale.ROOT);
        return "Запрос на " + (requestedMode.equals("on") ? "включение" : "выключение")
                + " сигналов принят. Настройка пока не подключена.";
    }

    private String validateTicker(String ticker, String command) {
        if (ticker.isBlank()) {
            return "Укажи тикер. Например: " + command + " SBER";
        }
        if (!ticker.matches("[A-Za-z0-9.-]{1,20}")) {
            return "Тикер должен содержать только латинские буквы, цифры, точку или дефис.";
        }
        return null;
    }
}
