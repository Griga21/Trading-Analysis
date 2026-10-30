package com.trading.service.interfecies;

import java.util.List;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.trading.model.CandleData;

public interface CandleService {

    CandleData getCandleBySecurityId(Long securityId);
    List<CandleData> getAllCandles();

    Page<CandleData> getCandles(String securityId, LocalDate from, LocalDate to, Pageable pageable);
}