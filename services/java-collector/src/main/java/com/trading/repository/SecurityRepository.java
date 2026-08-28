package com.trading.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trading.model.Security;

@Repository
public interface SecurityRepository extends JpaRepository<Security, String> {
    List<Security> findByActiveTrue();
}
