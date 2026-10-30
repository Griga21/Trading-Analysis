package com.trading.service.interfecies;

import java.util.List;
import com.trading.model.Security;

import jakarta.validation.constraints.NotNull;

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

    List<Security> getActiveSecurities();

    /**
     * Adds a new Security object.
     *
     * @param security The Security object to add.
     * @return The added Security object.
     */
    Security addSecurity(@NotNull Security security);

    Security addSecurityByTicker(String ticker);

    Security setActive(String securityId, boolean active);

    /**
     * Deletes a Security object by its ID.
     *
     * @param securityId The ID of the security to delete.
     * @return The deleted Security object.
     */
    String deleteSecurity(String securityId);
}
