package com.trading.test;

import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestMoexSimple {
    public static void main(String[] args) {
        // Простой URL
        String url = "https://iss.moex.com/iss/engines/stock/markets/shares/boards/TQBR/securities/SBER/candles.json?interval=24&from=2026-01-01&till=2026-12-31";
        
        System.out.println("URL: " + url);
        System.out.println("=== ЗАПРОС К MOEX API ===");
        
        try {
            // Получаем ответ
            WebClient client = WebClient.builder().build();
            String response = client.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            // Парсим JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode candles = root.get("candles");
            JsonNode columns = candles.get("columns");
            JsonNode data = candles.get("data");
            
            System.out.println("=== КОЛОНКИ ===");
            System.out.println(columns);
            
            System.out.println("\n=== ВСЕГО СТРОК: " + data.size() + " ===");
            
            // Первые 5 строк
            System.out.println("\n=== ПЕРВЫЕ 5 СТРОК ===");
            for (int i = 0; i < Math.min(5, data.size()); i++) {
                JsonNode row = data.get(i);
                System.out.println("Строка " + i + ": " + row);
                
                // Разберем по полям
                if (row.size() >= 6) {
                    System.out.println("  Open: " + row.get(0));
                    System.out.println("  Close: " + row.get(1));
                    System.out.println("  High: " + row.get(2));
                    System.out.println("  Low: " + row.get(3));
                    System.out.println("  Volume: " + row.get(4));
                    System.out.println("  Begin: " + row.get(5));
                    System.out.println("  End: " + row.get(6));
                }
                System.out.println();
            }
            
            // Последние 5 строк
            System.out.println("\n=== ПОСЛЕДНИЕ 5 СТРОК ===");
            for (int i = Math.max(0, data.size() - 5); i < data.size(); i++) {
                JsonNode row = data.get(i);
                System.out.println("Строка " + i + ": " + row);
            }
            
            // Проверим даты
            System.out.println("\n=== ДИАПАЗОН ДАТ ===");
            if (data.size() > 0) {
                String firstDate = data.get(0).get(5).asText();
                String lastDate = data.get(data.size() - 1).get(5).asText();
                System.out.println("Первая дата: " + firstDate);
                System.out.println("Последняя дата: " + lastDate);
                
                // Проверим есть ли 2026 год
                boolean has2026 = false;
                for (JsonNode row : data) {
                    if (row.get(5).asText().startsWith("2026")) {
                        has2026 = true;
                        break;
                    }
                }
                System.out.println("Есть данные за 2026 год: " + has2026);
            }
            
        } catch (Exception e) {
            System.err.println("ОШИБКА: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
