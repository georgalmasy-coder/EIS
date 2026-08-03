package com.bepa.eis.server.api.external.vies;

import com.bepa.eis.common.GlobalConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Pattern;

public class ViesVatLookupService {

    private static final Logger log = LoggerFactory.getLogger(ViesVatLookupService.class);
    private static final URI VIES_ENDPOINT = URI.create("https://ec.europa.eu/taxation_customs/vies/services/checkVatService");
    private static final Pattern COUNTRY_CODE_PATTERN = Pattern.compile("[A-Z]{2}");
    private static final Pattern VAT_NUMBER_PATTERN = Pattern.compile("[A-Z0-9]{2,20}");

    private final HttpClient httpClient;
    private final Duration timeout;

    public ViesVatLookupService() {
        this(Duration.ofSeconds(GlobalConfiguration.getViesValidationTimeoutSeconds()));
    }

    public ViesVatLookupService(Duration timeout) {
        this.timeout = timeout == null ? Duration.ofSeconds(GlobalConfiguration.getViesValidationTimeoutSeconds()) : timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.timeout)
                .build();
    }

    public ViesVatValidationResult validateVat(String countryCode, String vatNumber) {
        String normalizedCountryCode = normalizeCountryCode(countryCode);
        String normalizedVatNumber = normalizeVatNumber(vatNumber);

        log.info(
                "Starting VIES VAT validation. countryCode={}, vatNumber={}, timeoutSeconds={}",
                normalizedCountryCode,
                normalizedVatNumber,
                timeout == null ? null : timeout.getSeconds()
        );

        if (!COUNTRY_CODE_PATTERN.matcher(normalizedCountryCode).matches()) {
            throw new IllegalArgumentException("Country code must contain exactly 2 letters.");
        }

        if (!VAT_NUMBER_PATTERN.matcher(normalizedVatNumber).matches()) {
            throw new IllegalArgumentException("VAT number format is invalid.");
        }

        HttpRequest request = HttpRequest.newBuilder(VIES_ENDPOINT)
                .timeout(timeout)
                .header("Content-Type", "text/xml; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(
                        buildSoapRequest(normalizedCountryCode, normalizedVatNumber),
                        StandardCharsets.UTF_8
                ))
                .build();

        HttpResponse<String> response = sendRequest(request, normalizedCountryCode, normalizedVatNumber);

        log.info(
                "VIES VAT validation response received. countryCode={}, vatNumber={}, httpStatus={}",
                normalizedCountryCode,
                normalizedVatNumber,
                response.statusCode()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ViesVatLookupException(
                    "VIES returned HTTP " + response.statusCode() + ": " + response.body()
            );
        }

        ViesVatValidationResult validationResult = parseResponse(response.body(), normalizedCountryCode, normalizedVatNumber);

        log.info(
                "VIES VAT validation parsed. countryCode={}, vatNumber={}, valid={}, viesName={}, viesAddress={}",
                validationResult.countryCode(),
                validationResult.vatNumber(),
                validationResult.valid(),
                validationResult.name(),
                validationResult.address()
        );

        return validationResult;
    }

    private String buildSoapRequest(String countryCode, String vatNumber) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:urn="urn:ec.europa.eu:taxud:vies:services:checkVat:types">
                    <soapenv:Body>
                        <urn:checkVat>
                            <urn:countryCode>%s</urn:countryCode>
                            <urn:vatNumber>%s</urn:vatNumber>
                        </urn:checkVat>
                    </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(countryCode, vatNumber);
    }

    private HttpResponse<String> sendRequest(
            HttpRequest request,
            String countryCode,
            String vatNumber
    ) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info(
                    "VIES VAT validation interrupted. countryCode={}, vatNumber={}, message={}",
                    countryCode,
                    vatNumber,
                    e.getMessage(),
                    e
            );
            throw new ViesVatLookupException("VIES request was interrupted.", e);
        } catch (IOException e) {
            log.info(
                    "VIES VAT validation IO failure. countryCode={}, vatNumber={}, message={}",
                    countryCode,
                    vatNumber,
                    e.getMessage(),
                    e
            );
            throw new ViesVatLookupException("Could not call VIES.", e);
        }
    }

    private ViesVatValidationResult parseResponse(
            String responseBody,
            String countryCode,
            String vatNumber
    ) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new ViesVatLookupException("VIES returned an empty response.");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(responseBody)));

            String validText = textContent(document, "valid");
            boolean valid = "true".equalsIgnoreCase(validText) || "1".equals(validText);

            return new ViesVatValidationResult(
                    countryCode,
                    vatNumber,
                    valid,
                    textContent(document, "name"),
                    textContent(document, "address")
            );
        } catch (Exception e) {
            throw new ViesVatLookupException("Could not parse VIES response.", e);
        }
    }

    private String textContent(Document document, String localName) {
        if (document == null) {
            return "";
        }

        NodeList nodes = document.getElementsByTagNameNS("*", localName);

        if (nodes == null || nodes.getLength() == 0) {
            return "";
        }

        Node node = nodes.item(0);

        if (node == null || node.getTextContent() == null) {
            return "";
        }

        return node.getTextContent().trim();
    }

    private String normalizeCountryCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeVatNumber(String value) {
        return value == null ? "" : value.replaceAll("[\\s.-]+", "").toUpperCase(Locale.ROOT);
    }

    public record ViesVatValidationResult(
            String countryCode,
            String vatNumber,
            boolean valid,
            String name,
            String address
    ) {
    }

    public static class ViesVatLookupException extends RuntimeException {
        public ViesVatLookupException(String message) {
            super(message);
        }

        public ViesVatLookupException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
