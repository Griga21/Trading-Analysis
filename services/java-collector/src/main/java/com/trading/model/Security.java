package com.trading.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "securities")
@Getter
@Setter
public class Security {

    @Id
    @Column(name = "security_id", length = 20, nullable = false)
    private String securityId;

    @Column(name = "short_name")
    private String shortName;

    @Column(nullable = false)
    private String board = "TQBR";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
