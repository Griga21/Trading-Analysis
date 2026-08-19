// CandleData.java
package com.trading.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "candles")
@Data
public class CandleData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "security_id", nullable = false)
    private String securityId;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Double volume;
}