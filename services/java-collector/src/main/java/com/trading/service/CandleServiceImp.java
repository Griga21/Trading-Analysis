package com.trading.service;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.trading.customExceptions.SecurityNotFoundException;
import com.trading.model.CandleData;
import com.trading.service.interfecies.CandleService;
import com.trading.repository.CandleRepository;
import com.trading.repository.SecurityRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class CandleServiceImp implements CandleService {

    private final CandleRepository candleRepository;
    private final SecurityRepository securityRepository;

    @Override
    public CandleData getCandleBySecurityId(Long securityId) {
        return candleRepository.findBySecurityIdOrderByTimestampDesc(String.valueOf(securityId)).stream().findFirst().orElse(null);
    }

    @Override
    public List<CandleData> getAllCandles() {
        return candleRepository.findAll();
    }

    @Override
    public Page<CandleData> getCandles(String securityId, LocalDate from, LocalDate to, Pageable pageable) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Both from and to dates are required.");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date must not be after 'to' date.");
        }
        if (!securityRepository.existsById(securityId)) {
            throw new SecurityNotFoundException("Security with ID " + securityId);
        }

        return candleRepository
                .findBySecurityIdAndTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampAsc(
                        securityId,
                        from.atStartOfDay(),
                        to.plusDays(1).atStartOfDay(),
                        pageable);
    }
    
}
