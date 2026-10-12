package com.trading.service;

import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.trading.model.Security;
import com.trading.repository.SecurityRepository;
import com.trading.service.interfecies.SecuritiesService;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class SecuritiesServiceImpl implements SecuritiesService {

    private final SecurityRepository securityRepository;

    @Override
    public Security getSecurityById(String securityId) {
        return securityRepository.findById(securityId).orElse(null);
    }

    @Override
    public List<Security> getAllSecurities() {
        return securityRepository.findAll();
    }

    @Override
    public Security addSecurity(@NotNull Security security) {
        return securityRepository.save(security);
    }

    @Override
    public String deleteSecurity(String securityId) {
        securityRepository.deleteById(securityId);
        return securityId;
    }
}