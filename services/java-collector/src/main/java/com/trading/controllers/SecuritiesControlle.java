package com.trading.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trading.model.Security;
import com.trading.service.interfecies.SecuritiesService;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/securities")
@RequiredArgsConstructor 
public class SecuritiesControlle {

    private final SecuritiesService securitiesService;

    @GetMapping ("/getById")
    public Security getSecurityById(@RequestParam String securityId) {
        if(securityId == null || securityId.isEmpty()) {
            throw new IllegalArgumentException("Security ID cannot be null or empty.");
        }
        return securitiesService.getSecurityById(securityId);
    }

    
    @GetMapping ("/getAll")
    public List<Security> getAllSecurities() {
        return securitiesService.getAllSecurities();
    }

    @PostMapping ("/add")
    public Security addSecurity(@RequestBody Security security) {
        return securitiesService.addSecurity(security);
    }

    @PostMapping("/add-by-ticker")
    public Security addSecurityByTicker(@RequestParam String ticker) {
        return securitiesService.addSecurityByTicker(ticker);
    }

    @DeleteMapping ("/delete")
    public void deleteSecurity(@RequestParam String securityId) {
        Security security = securitiesService.getSecurityById(securityId);
        if (security != null) {
            securitiesService.deleteSecurity(securityId);
        }
    }
    
}
