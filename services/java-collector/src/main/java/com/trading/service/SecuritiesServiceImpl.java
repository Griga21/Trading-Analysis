package com.trading.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.trading.customExceptions.SecuritiesAlreadyExistsException;
import com.trading.customExceptions.SecurityNotFoundException;
import com.trading.dto.MoexSecurityInfo;
import com.trading.model.Security;
import com.trading.repository.SecurityRepository;
import com.trading.service.interfecies.SecuritiesService;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class SecuritiesServiceImpl implements SecuritiesService {

    private final SecurityRepository securityRepository;
    private final MoexDataService moexDataService;

    @Override
    public Security getSecurityById(String securityId) {
        return securityRepository.findById(securityId)
                    .orElseThrow(() -> 
                        new SecurityNotFoundException("Security with ID " + securityId + " not found."));
    }

    @Override
    public List<Security> getAllSecurities() {
        return securityRepository.findAll();
    }

    @Override
    public Security addSecurity(@NotNull Security security) {
        if (securityRepository.existsById(security.getSecurityId())) {
            throw new SecuritiesAlreadyExistsException("Security with ID " + security.getSecurityId());
        }
        return securityRepository.save(security);
    }

    @Override
    public Security addSecurityByTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker cannot be null or blank.");
        }

        String normalizedTicker = ticker.trim().toUpperCase(Locale.ROOT);
        if (!normalizedTicker.matches("[A-Z0-9.-]{1,20}")) {
            throw new IllegalArgumentException("Ticker contains invalid characters.");
        }

        if (securityRepository.existsById(normalizedTicker)) {
            throw new SecuritiesAlreadyExistsException("Security with ID " + normalizedTicker);
        }

        MoexSecurityInfo info = moexDataService.fetchSecurityInfo(normalizedTicker);
        Security security = new Security();
        security.setSecurityId(info.securityId());
        security.setShortName(info.shortName());
        security.setBoard(info.board());
        security.setActive(true);

        return securityRepository.save(security);
    }

    @Override
    public String deleteSecurity(String securityId) {
        securityRepository.deleteById(securityId);
        return securityId;
    }
}