package com.trading.controllers;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.trading.dto.ActiveStatusRequest;
import com.trading.dto.CandlePageResponse;
import com.trading.dto.CreateSecurityRequest;
import com.trading.dto.SecurityResponse;
import com.trading.model.CandleData;
import com.trading.model.Security;
import com.trading.service.interfecies.CandleService;
import com.trading.service.interfecies.SecuritiesService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/securities")
@RequiredArgsConstructor
@Validated
public class ApiSecuritiesController {

    private final SecuritiesService securitiesService;
    private final CandleService candleService;

    @GetMapping
    public List<SecurityResponse> getSecurities(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        List<Security> securities = activeOnly
                ? securitiesService.getActiveSecurities()
                : securitiesService.getAllSecurities();
        return securities.stream().map(SecurityResponse::from).toList();
    }

    @GetMapping("/{ticker}")
    public SecurityResponse getSecurity(@PathVariable String ticker) {
        return SecurityResponse.from(securitiesService.getSecurityById(normalizeTicker(ticker)));
    }

    @PostMapping
    public ResponseEntity<SecurityResponse> addSecurity(@Valid @RequestBody CreateSecurityRequest request) {
        Security security = securitiesService.addSecurityByTicker(request.ticker());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{ticker}")
                .buildAndExpand(security.getSecurityId())
                .toUri();
        return ResponseEntity.created(location).body(SecurityResponse.from(security));
    }

    @PatchMapping("/{ticker}/active")
    public SecurityResponse updateActiveStatus(
            @PathVariable String ticker,
            @Valid @RequestBody ActiveStatusRequest request) {
        return SecurityResponse.from(
                securitiesService.setActive(normalizeTicker(ticker), request.active()));
    }

    @DeleteMapping("/{ticker}")
    public ResponseEntity<Void> deactivateSecurity(@PathVariable String ticker) {
        securitiesService.setActive(normalizeTicker(ticker), false);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{ticker}/candles")
    public CandlePageResponse getCandles(
            @PathVariable String ticker,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "500") @Min(1) @Max(1000) int size) {
        Page<CandleData> candles = candleService.getCandles(
                normalizeTicker(ticker),
                from,
                to,
                PageRequest.of(page, size));
        return CandlePageResponse.from(candles);
    }

    private String normalizeTicker(String ticker) {
        return ticker.trim().toUpperCase(Locale.ROOT);
    }
}
