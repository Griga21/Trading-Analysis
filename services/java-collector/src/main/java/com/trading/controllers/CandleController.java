package com.trading.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.trading.model.CandleData;
import com.trading.service.interfecies.CandleService;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/candles")
@RequiredArgsConstructor 
public class CandleController {

    private final CandleService candleService;

    @GetMapping ("/candles/getBySecurityId")
    public CandleData getCandleBySecurityId(Long securityId) {
        return candleService.getCandleBySecurityId(securityId);
    }

    @GetMapping ("/candles/getAll")
    public List<CandleData> getCandles() {
        return candleService.getAllCandles();
    }
}
