package com.telegram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TelegramBot {
    public static void main(String[] args) {
        SpringApplication.run(TelegramBot.class, args);
        System.out.println("Telgram Bot is starting!");
    }
}
