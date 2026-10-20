package com.telegram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling 
public class TelegramBot {
    public static void main(String[] args) {
        SpringApplication.run(TelegramBot.class, args);
        System.out.println("Telgram Bot is starting!");
    }
}
