package com.trading.service;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.trading.model.Security;
import com.trading.repository.SecurityRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecurityService {

    private final SecurityRepository securityRepository;

    @Transactional
    public void saveIfAbsent(@NonNull String securityId) {
        if (!securityRepository.existsById(securityId)) {
            Security security = new Security();
            security.setSecurityId(securityId);
            security.setBoard("TQBR");
            security.setActive(true);

            securityRepository.save(security);
        }
    }
}
