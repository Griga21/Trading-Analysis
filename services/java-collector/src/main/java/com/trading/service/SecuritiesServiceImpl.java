package com.trading.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.trading.customExceptions.SecuritiesAlreadyExistsException;
import com.trading.customExceptions.SecurityNotFoundException;
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
    public String deleteSecurity(String securityId) {
        securityRepository.deleteById(securityId);
        return securityId;
    }
}