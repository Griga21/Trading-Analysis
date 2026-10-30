package com.telegram.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TelegramCommandHandlerTest {

    private final TelegramCommandHandler handler = new TelegramCommandHandler();

    @Test
    void helpListsSupportedCommands() {
        String response = handler.handle("/help");

        assertTrue(response.contains("/addNew <тикер>"));
        assertTrue(response.contains("/remove <тикер>"));
        assertTrue(response.contains("/list"));
        assertTrue(response.contains("/status <тикер>"));
        assertTrue(response.contains("/signals on|off"));
    }

    @Test
    void addCommandValidatesAndNormalizesTickerWithoutAddingIt() {
        assertTrue(handler.handle("/addNew sber").contains("SBER"));
        assertTrue(handler.handle("/addNew").contains("Укажи тикер"));
        assertTrue(handler.handle("/addNew SBER!").contains("Тикер должен"));
        assertTrue(handler.handle("/addNew SBER").contains("пока не подключено"));
    }

    @Test
    void removeAndStatusCommandsAreRecognizedAsStubs() {
        assertTrue(handler.handle("/remove gazp").contains("убрать из отслеживания GAZP"));
        assertTrue(handler.handle("/status LKOH").contains("показывать статус LKOH"));
    }

    @Test
    void listAndSignalsCommandsValidateArguments() {
        assertTrue(handler.handle("/list").contains("пока недоступен"));
        assertTrue(handler.handle("/list SBER").contains("не принимает аргументы"));
        assertTrue(handler.handle("/signals on").contains("включение"));
        assertTrue(handler.handle("/signals off").contains("выключение"));
        assertTrue(handler.handle("/signals maybe").contains("/signals on"));
    }

    @Test
    void acceptsTelegramCommandUsernameSuffixAndDistinguishesUnknownText() {
        assertTrue(handler.handle("/list@market_bot").contains("пока недоступен"));
        assertTrue(handler.handle("/something").contains("Неизвестная команда"));
        assertTrue(handler.handle("обычный текст").contains("понимаю только команды"));
    }
}
