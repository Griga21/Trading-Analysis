package com.trading.repository;

import com.trading.model.CandleData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CandleRepository extends JpaRepository<CandleData, Long> {

    List<CandleData> findBySecurityIdOrderByTimestampDesc(String securityId);
    
    boolean existsBySecurityIdAndTimestamp(String securityId, LocalDateTime timestamp);
}