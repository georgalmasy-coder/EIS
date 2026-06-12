package com.bepa.eis.server.api.external.virk.cvr;

/**
 * Exception for fejl ved opslag mod CVR API.
 */
public class CvrApiException extends RuntimeException {

    public CvrApiException(String message) {
        super(message);
    }

    public CvrApiException(String message, Throwable cause) {
        super(message, cause);
    }
}