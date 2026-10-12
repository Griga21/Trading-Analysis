package com.trading.service.interfecies;

import java.util.List;
import com.trading.model.CandleData;

public interface CandleService {

    CandleData getCandleBySecurityId(Long securityId);
    List<CandleData> getAllCandles();
}