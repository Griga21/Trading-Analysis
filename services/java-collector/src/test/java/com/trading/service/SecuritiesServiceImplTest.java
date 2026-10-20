package com.trading.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.trading.customExceptions.SecuritiesAlreadyExistsException;
import com.trading.dto.MoexSecurityInfo;
import com.trading.model.Security;
import com.trading.repository.SecurityRepository;

@ExtendWith(MockitoExtension.class)
class SecuritiesServiceImplTest {

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private MoexDataService moexDataService;

    @InjectMocks
    private SecuritiesServiceImpl securitiesService;

    @Test
    void addsSecurityUsingMoexInformation() {
        when(securityRepository.existsById("SBER")).thenReturn(false);
        when(moexDataService.fetchSecurityInfo("SBER"))
                .thenReturn(new MoexSecurityInfo("SBER", "Сбербанк", "TQBR"));
        when(securityRepository.save(org.mockito.ArgumentMatchers.any(Security.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Security saved = securitiesService.addSecurityByTicker(" sber ");

        assertEquals("SBER", saved.getSecurityId());
        assertEquals("Сбербанк", saved.getShortName());
        assertEquals("TQBR", saved.getBoard());
        assertEquals(true, saved.isActive());
        verify(securityRepository).save(saved);
    }

    @Test
    void rejectsExistingTickerBeforeCallingMoex() {
        when(securityRepository.existsById("SBER")).thenReturn(true);

        assertThrows(
                SecuritiesAlreadyExistsException.class,
                () -> securitiesService.addSecurityByTicker("sber"));

        verify(moexDataService, never()).fetchSecurityInfo("SBER");
    }

    @Test
    void rejectsInvalidTicker() {
        assertThrows(
                IllegalArgumentException.class,
                () -> securitiesService.addSecurityByTicker("SBER;DROP"));
    }
}
