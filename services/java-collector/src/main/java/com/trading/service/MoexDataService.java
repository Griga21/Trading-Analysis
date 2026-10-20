package com.trading.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.model.CandleData;
import com.trading.dto.MoexSecurityInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class MoexDataService {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${moex.api.base-url:https://iss.moex.com/iss}")
    private String baseUrl;

    public MoexDataService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public MoexSecurityInfo fetchSecurityInfo(String securityId) {
        String normalizedSecurityId = securityId.trim().toUpperCase(Locale.ROOT);
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .pathSegment("engines", "stock", "markets", "shares", "boards", "TQBR",
                        "securities", normalizedSecurityId + ".json")
                .queryParam("iss.meta", "off")
                .queryParam("iss.only", "securities")
                .queryParam("securities.columns", "SECID,SHORTNAME")
                .build()
                .encode()
                .toUri();

        JsonNode response = webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(10));

        if (response == null) {
            throw new IllegalStateException("MOEX returned an empty response for " + normalizedSecurityId);
        }

        JsonNode table = response.path("securities");
        JsonNode columns = table.path("columns");
        JsonNode rows = table.path("data");
        int securityIdIndex = findColumnIndex(columns, "SECID");
        int shortNameIndex = findColumnIndex(columns, "SHORTNAME");

        if (!rows.isArray() || securityIdIndex < 0 || shortNameIndex < 0) {
            throw new IllegalStateException("MOEX response does not contain security data for " + normalizedSecurityId);
        }

        for (JsonNode row : rows) {
            JsonNode returnedSecurityId = row.path(securityIdIndex);
            if (returnedSecurityId.asText().equalsIgnoreCase(normalizedSecurityId)) {
                String shortName = row.path(shortNameIndex).asText();
                if (shortName.isBlank()) {
                    throw new IllegalStateException("MOEX returned no short name for " + normalizedSecurityId);
                }
                return new MoexSecurityInfo(normalizedSecurityId, shortName, "TQBR");
            }
        }

        throw new IllegalArgumentException("Security " + normalizedSecurityId + " was not found on MOEX board TQBR");
    }

    private int findColumnIndex(JsonNode columns, String expectedName) {
        if (!columns.isArray()) {
            return -1;
        }
        for (int i = 0; i < columns.size(); i++) {
            if (expectedName.equals(columns.get(i).asText())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Получение свечей для конкретной бумаги
     */
    public List<CandleData> fetchCandles(String security, String from, String till) {
        log.info("Loading candles for {} from {} to {}", security, from, till);
        List<CandleData> allCandles = new ArrayList<>();
        int start = 0;

        while (true) {
            String url = String.format(
                    "%s/engines/stock/markets/shares/boards/TQBR/securities/%s/candles.json?interval=24&from=%s&till=%s&start=%d",
                    baseUrl, security, from, till, start);

            String response = webClient.get()
                    .uri(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            List<CandleData> batch = parseCandles(response, security);
            if (batch.isEmpty()) {
                break;
            }
            allCandles.addAll(batch);
            start += 500; // размер страницы MOEX для candles.json
            log.info("Fetched batch of {} for {}, total so far: {}", batch.size(), security, allCandles.size());
        }

        return allCandles;
    }

    /**
     * Получение свечей за последние N дней
     */
    public List<CandleData> fetchRecentCandles(String security, int days) {
        String from = LocalDateTime.now().minusDays(days).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        String till = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        return fetchCandles(security, from, till);
    }

    /**
     * Получение свечей за последние 30 дней
     */
    public List<CandleData> fetchRecentCandles(String security) {
        return fetchRecentCandles(security, 30);
    }

    /**
     * Получение списка всех доступных акций
     */
    public List<String> getAvailableSecurities() {
        String path = "/engines/stock/markets/shares/boards/TQBR/securities.json";

        try {
            String response = webClient.get()
                    .uri(baseUrl + path)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseSecurities(response);
        } catch (Exception e) {
            log.error("Error getting list of securities: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Получение текущих котировок
     */
    public JsonNode getCurrentQuotes(String security) {
        String path = String.format(
                "/engines/stock/markets/shares/boards/TQBR/securities/%s.json",
                security);

        try {
            String response = webClient.get()
                    .uri(baseUrl + path)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return objectMapper.readTree(response);
        } catch (Exception e) {
            log.error("Error getting quotes for {}: {}", security, e.getMessage());
            return null;
        }
    }

    /**
     * Парсинг свечей из JSON ответа
     */
    private List<CandleData> parseCandles(String response, String security) {
        List<CandleData> candles = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.get("candles").get("data");
            JsonNode columns = root.get("candles").get("columns");

            int openIdx = -1, closeIdx = -1, highIdx = -1,
                    lowIdx = -1, volumeIdx = -1, timeIdx = -1;

            for (int i = 0; i < columns.size(); i++) {
                String colName = columns.get(i).asText();
                switch (colName) {
                    case "open":
                        openIdx = i;
                        break;
                    case "close":
                        closeIdx = i;
                        break;
                    case "high":
                        highIdx = i;
                        break;
                    case "low":
                        lowIdx = i;
                        break;
                    case "volume":
                        volumeIdx = i;
                        break;
                    case "begin":
                        timeIdx = i;
                        break;
                }
            }

            // Используйте правильный формат даты
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (JsonNode candle : data) {
                try {
                    CandleData candleData = new CandleData();
                    candleData.setSecurityId(security);

                    // Правильный парсинг даты
                    String timeStr = candle.get(timeIdx).asText();
                    LocalDateTime timestamp = LocalDateTime.parse(timeStr, formatter);
                    candleData.setTimestamp(timestamp);

                    candleData.setOpen(candle.get(openIdx).asDouble());
                    candleData.setHigh(candle.get(highIdx).asDouble());
                    candleData.setLow(candle.get(lowIdx).asDouble());
                    candleData.setClose(candle.get(closeIdx).asDouble());
                    candleData.setVolume(candle.get(volumeIdx).asDouble());

                    candles.add(candleData);
                } catch (Exception e) {
                    log.warn("Error parsing candle: {}", e.getMessage());
                }
            }

            log.info("Loaded {} candles for {}", candles.size(), security);

        } catch (Exception e) {
            log.error("Error parsing response: {}", e.getMessage());
        }

        return candles;
    }

    /**
     * Парсинг списка доступных акций
     */
    private List<String> parseSecurities(String response) {
        List<String> securities = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.get("securities").get("data");
            JsonNode columns = root.get("securities").get("columns");

            // Находим индекс SECID
            int secIdIdx = -1;
            for (int i = 0; i < columns.size(); i++) {
                if ("SECID".equals(columns.get(i).asText())) {
                    secIdIdx = i;
                    break;
                }
            }

            if (secIdIdx >= 0) {
                for (JsonNode row : data) {
                    securities.add(row.get(secIdIdx).asText());
                }
            }

            log.info("Loaded list of {} securities", securities.size());

        } catch (Exception e) {
            log.error("Error parsing list of securities: {}", e.getMessage());
        }

        return securities;
    }
}