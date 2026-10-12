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

    @GetMapping ("/securities/getById")
    public Security getSecurityById(String securityId) {
        return securitiesService.getSecurityById(securityId);
    }

    
    @GetMapping ("/securities/getAll")
    public List<Security> getAllSecurities() {
        return securitiesService.getAllSecurities();
    }

    @PostMapping ("/securities/add")
    public Security addSecurity(@RequestBody Security security) {
        return securitiesService.addSecurity(security);
    }

    @DeleteMapping ("/securities/delete")
    public void deleteSecurity(@RequestParam String securityId) {
        Security security = securitiesService.getSecurityById(securityId);
        if (security != null) {
            securitiesService.deleteSecurity(securityId);
        }
    }
    
}
