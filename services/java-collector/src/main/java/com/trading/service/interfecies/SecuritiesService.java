package com.trading.service.interfecies;

import java.util.List;
import com.trading.model.Security;

public interface SecuritiesService {
    /**
     * Retrieves a Security object by its ID.
     *
     * @param securityId The ID of the security to retrieve.
     * @return The Security object corresponding to the provided ID, or null if not found.
     */
    Security getSecurityById(String securityId);
    
    /**
     * Retrieves all Security objects.
     *
     * @return A list of all Security objects.
     */
    List<Security> getAllSecurities();
}
