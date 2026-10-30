package com.trading.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.trading.customExceptions.SecuritiesAlreadyExistsException;
import com.trading.model.Security;
import com.trading.model.CandleData;
import com.trading.service.interfecies.CandleService;
import com.trading.service.interfecies.SecuritiesService;

@WebMvcTest(ApiSecuritiesController.class)
@Import(ApiExceptionHandler.class)
class ApiSecuritiesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SecuritiesService securitiesService;

    @MockBean
    private CandleService candleService;

    @Test
    void createsSecurityFromTickerAndReturnsLocation() throws Exception {
        Security security = security("SBER", "Сбербанк", true);
        when(securitiesService.addSecurityByTicker("sber")).thenReturn(security);

        mockMvc.perform(post("/api/v1/securities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"sber\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/securities/SBER"))
                .andExpect(jsonPath("$.ticker").value("SBER"))
                .andExpect(jsonPath("$.shortName").value("Сбербанк"))
                .andExpect(jsonPath("$.active").value(true));

        verify(securitiesService).addSecurityByTicker("sber");
    }

    @Test
    void rejectsInvalidTickerRequest() throws Exception {
        mockMvc.perform(post("/api/v1/securities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"SBER!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void returnsConflictForDuplicateTicker() throws Exception {
        when(securitiesService.addSecurityByTicker("SBER"))
                .thenThrow(new SecuritiesAlreadyExistsException("SBER"));

        mockMvc.perform(post("/api/v1/securities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"SBER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void listsActiveSecuritiesWhenRequested() throws Exception {
        when(securitiesService.getActiveSecurities())
                .thenReturn(List.of(security("SBER", "Сбербанк", true)));

        mockMvc.perform(get("/api/v1/securities").param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("SBER"));
    }

    @Test
    void updatesActiveStatusWithoutDeletingSecurity() throws Exception {
        when(securitiesService.setActive("SBER", false))
                .thenReturn(security("SBER", "Сбербанк", false));

        mockMvc.perform(patch("/api/v1/securities/sber/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(securitiesService).setActive("SBER", false);
    }

    @Test
    void returnsCandlesForInclusiveDateRangeWithPagination() throws Exception {
        CandleData candle = new CandleData();
        candle.setSecurityId("SBER");
        candle.setTimestamp(java.time.LocalDateTime.of(2026, 10, 20, 0, 0));
        candle.setOpen(100.0);
        candle.setHigh(105.0);
        candle.setLow(99.0);
        candle.setClose(103.0);
        candle.setVolume(1000.0);

        when(candleService.getCandles(
                eq("SBER"),
                eq(LocalDate.of(2026, 10, 1)),
                eq(LocalDate.of(2026, 10, 31)),
                any()))
                .thenReturn(new PageImpl<>(List.of(candle), PageRequest.of(0, 100), 1));

        mockMvc.perform(get("/api/v1/securities/sber/candles")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].ticker").value("SBER"))
                .andExpect(jsonPath("$.items[0].close").value(103.0))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void rejectsCandlePageSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/api/v1/securities/SBER/candles")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31")
                        .param("size", "1001"))
                .andExpect(status().isBadRequest());
    }

    private Security security(String ticker, String shortName, boolean active) {
        Security security = new Security();
        security.setSecurityId(ticker);
        security.setShortName(shortName);
        security.setBoard("TQBR");
        security.setActive(active);
        return security;
    }
}
