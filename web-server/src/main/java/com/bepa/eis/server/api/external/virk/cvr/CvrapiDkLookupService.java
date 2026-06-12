package com.bepa.eis.server.api.external.virk.cvr;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * CVR lookup service baseret på cvrapi.dk.
 */
public class CvrapiDkLookupService implements CvrLookupService {

    private static final Pattern CVR_PATTERN = Pattern.compile("\\d{8}");

    private final CvrApiConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CvrapiDkLookupService() {
        this(CvrApiConfig.defaultConfig());
    }

    public CvrapiDkLookupService(CvrApiConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.getTimeout())
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Optional<CvrCompanyDto> findCompanyByCvrNumber(String cvrNumber) {
        String normalizedCvrNumber = normalizeAndValidateCvrNumber(cvrNumber);
        URI requestUri = buildRequestUri(normalizedCvrNumber);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(requestUri)
                .timeout(config.getTimeout())
                .header("Accept", "application/json")
                .header("User-Agent", config.getUserAgent())
                .GET()
                .build();

        HttpResponse<String> response = sendRequest(request);

        if (response.statusCode() == 404) {
            return Optional.empty();
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new CvrApiException(
                    "CVR API returned HTTP " + response.statusCode() + ": " + response.body()
            );
        }

        CvrApiResponseDto apiResponse = readApiResponse(response.body());

        if (isNotFoundOrError(apiResponse)) {
            return Optional.empty();
        }

        return Optional.ofNullable(CvrCompanyDto.fromApiResponse(apiResponse));
    }

    private String normalizeAndValidateCvrNumber(String cvrNumber) {
        if (cvrNumber == null) {
            throw new IllegalArgumentException("CVR-nummer må ikke være null");
        }

        String normalized = cvrNumber.replaceAll("\\s+", "");

        if (!CVR_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("CVR-nummer skal bestå af præcis 8 cifre");
        }

        return normalized;
    }

    private URI buildRequestUri(String cvrNumber) {
        String encodedSearch = URLEncoder.encode(cvrNumber, StandardCharsets.UTF_8);
        String encodedCountry = URLEncoder.encode("dk", StandardCharsets.UTF_8);

        return URI.create(config.getBaseUri() + "?search=" + encodedSearch + "&country=" + encodedCountry);
    }

    private HttpResponse<String> sendRequest(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new CvrApiException("Fejl ved kald til CVR API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CvrApiException("CVR API kald blev afbrudt", e);
        }
    }

    private CvrApiResponseDto readApiResponse(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, CvrApiResponseDto.class);
        } catch (IOException e) {
            throw new CvrApiException("Kunne ikke parse svar fra CVR API", e);
        }
    }

    private boolean isNotFoundOrError(CvrApiResponseDto apiResponse) {
        if (apiResponse == null) {
            return true;
        }

        if (apiResponse.getVat() == null || apiResponse.getVat().isBlank()) {
            return true;
        }

        String status = apiResponse.getStatus();

        return status != null
                && !status.isBlank()
                && !"200".equals(status)
                && !"OK".equalsIgnoreCase(status);
    }
}