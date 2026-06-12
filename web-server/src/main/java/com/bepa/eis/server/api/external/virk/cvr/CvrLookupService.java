package com.bepa.eis.server.api.external.virk.cvr;

import java.util.Optional;

/**
 * Service-interface for CVR-opslag.
 */
public interface CvrLookupService {

    /**
     * Slår en dansk virksomhed op på CVR-nummer.
     *
     * @param cvrNumber CVR-nummer, typisk 8 cifre.
     * @return virksomhedsoplysninger hvis fundet
     */
    Optional<CvrCompanyDto> findCompanyByCvrNumber(String cvrNumber);
}