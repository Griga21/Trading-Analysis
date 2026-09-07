package com.trading.repository;

import com.trading.model.CandleData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Modifying
    @Query(value = """
            INSERT INTO candles (
                security_id,
                timestamp,
                open,
                high,
                low,
                close,
                volume
            )
            VALUES (
                :securityId,
                :timestamp,
                :open,
                :high,
                :low,
                :close,
                :volume
            )
            ON CONFLICT (security_id, timestamp)
            DO UPDATE SET
                open = EXCLUDED.open,
                high = EXCLUDED.high,
                low = EXCLUDED.low,
                close = EXCLUDED.close,
                volume = EXCLUDED.volume
            """, nativeQuery = true)
    int upsertCandle(
            @Param("securityId") String securityId,
            @Param("timestamp") LocalDateTime timestamp,
            @Param("open") Double open,
            @Param("high") Double high,
            @Param("low") Double low,
            @Param("close") Double close,
            @Param("volume") Double volume);
}