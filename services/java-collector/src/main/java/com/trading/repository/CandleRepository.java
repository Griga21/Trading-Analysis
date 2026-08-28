package com.trading.repository;

import com.trading.model.CandleData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CandleRepository extends JpaRepository<CandleData, Long> {

    List<CandleData> findBySecurityIdOrderByTimestampDesc(String securityId);
    boolean existsBySecurityIdAndTimestamp(String securityId, LocalDateTime timestamp);
    boolean existsBySecurityId(String securityId);
    Optional<CandleData> findFirstBySecurityIdOrderByTimestampDesc(String securityId);
}