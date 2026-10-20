package com.telegram.service;


public interface NotificationService {
    void sendNotification(String message);

    void sendNotification(String chatId, String message);
}
