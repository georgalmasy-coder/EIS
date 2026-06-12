package com.bepa.eis.common.providers.security;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class MfaTotpService {

    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int QR_CODE_SIZE = 220;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSecret() {
        byte[] secretBytes = new byte[MfaConfig.getSecretBytes()];
        secureRandom.nextBytes(secretBytes);
        return encodeBase32(secretBytes);
    }

    public String buildOtpAuthUri(
            String issuer,
            String accountName,
            String secret
    ) {
        String safeIssuer = issuer == null || issuer.isBlank()
                ? MfaConfig.getIssuer()
                : issuer.trim();

        String safeAccountName = accountName == null || accountName.isBlank()
                ? "user"
                : accountName.trim();

        String safeSecret = normalizeSecret(secret);

        String label = urlEncode(safeIssuer + ":" + safeAccountName);

        return "otpauth://totp/" + label
                + "?secret=" + urlEncode(safeSecret)
                + "&issuer=" + urlEncode(safeIssuer)
                + "&algorithm=SHA1"
                + "&digits=" + MfaConfig.getCodeLength()
                + "&period=" + MfaConfig.getCodeValidSeconds();
    }

    public String buildOtpAuthQrCodeDataUri(String otpAuthUri) {
        if (otpAuthUri == null || otpAuthUri.isBlank()) {
            return null;
        }

        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    otpAuthUri,
                    BarcodeFormat.QR_CODE,
                    QR_CODE_SIZE,
                    QR_CODE_SIZE,
                    hints
            );

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

                String base64Png = Base64.getEncoder().encodeToString(outputStream.toByteArray());
                return "data:image/png;base64," + base64Png;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public boolean verifyCode(
            String secret,
            String code
    ) {
        return verifyCode(
                secret,
                code,
                Instant.now(),
                MfaConfig.getAllowedTimeWindowDrift()
        );
    }

    public boolean verifyCode(
            String secret,
            String code,
            Instant timestamp,
            int allowedTimeWindowDrift
    ) {
        if (secret == null || secret.isBlank()) {
            return false;
        }

        if (code == null || code.isBlank()) {
            return false;
        }

        String normalizedCode = code.trim();

        if (!normalizedCode.matches("\\d{" + MfaConfig.getCodeLength() + "}")) {
            return false;
        }

        Instant safeTimestamp = timestamp != null ? timestamp : Instant.now();
        int safeAllowedTimeWindowDrift = Math.max(allowedTimeWindowDrift, 0);

        long timeStep = safeTimestamp.getEpochSecond() / MfaConfig.getCodeValidSeconds();

        for (int offset = -safeAllowedTimeWindowDrift; offset <= safeAllowedTimeWindowDrift; offset++) {
            String expectedCode = generateCode(secret, timeStep + offset);

            if (constantTimeEquals(expectedCode, normalizedCode)) {
                return true;
            }
        }

        return false;
    }

    public String generateCurrentCodeForTest(String secret) {
        long timeStep = Instant.now().getEpochSecond() / MfaConfig.getCodeValidSeconds();
        return generateCode(secret, timeStep);
    }

    private String generateCode(
            String secret,
            long timeStep
    ) {
        try {
            byte[] secretBytes = decodeBase32(normalizeSecret(secret));
            byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeStep).array();

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGORITHM));

            byte[] hash = mac.doFinal(timeBytes);

            int offset = hash[hash.length - 1] & 0x0F;

            int binary =
                    ((hash[offset] & 0x7F) << 24)
                            | ((hash[offset + 1] & 0xFF) << 16)
                            | ((hash[offset + 2] & 0xFF) << 8)
                            | (hash[offset + 3] & 0xFF);

            int divisor = (int) Math.pow(10, MfaConfig.getCodeLength());
            int otp = binary % divisor;

            return String.format(Locale.ROOT, "%0" + MfaConfig.getCodeLength() + "d", otp);
        } catch (Exception e) {
            return "";
        }
    }

    private String encodeBase32(byte[] data) {
        StringBuilder result = new StringBuilder();

        int buffer = 0;
        int bitsLeft = 0;

        for (byte value : data) {
            buffer = (buffer << 8) | (value & 0xFF);
            bitsLeft += 8;

            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                result.append(BASE32_ALPHABET.charAt(index));
            }
        }

        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            result.append(BASE32_ALPHABET.charAt(index));
        }

        return result.toString();
    }

    private byte[] decodeBase32(String value) {
        String normalizedValue = normalizeSecret(value);

        int buffer = 0;
        int bitsLeft = 0;
        byte[] output = new byte[normalizedValue.length() * 5 / 8];
        int outputIndex = 0;

        for (int i = 0; i < normalizedValue.length(); i++) {
            char current = normalizedValue.charAt(i);
            int alphabetIndex = BASE32_ALPHABET.indexOf(current);

            if (alphabetIndex < 0) {
                throw new IllegalArgumentException("Invalid Base32 character: " + current);
            }

            buffer = (buffer << 5) | alphabetIndex;
            bitsLeft += 5;

            if (bitsLeft >= 8) {
                output[outputIndex++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }

        if (outputIndex == output.length) {
            return output;
        }

        byte[] trimmedOutput = new byte[outputIndex];
        System.arraycopy(output, 0, trimmedOutput, 0, outputIndex);
        return trimmedOutput;
    }

    private String normalizeSecret(String secret) {
        if (secret == null) {
            return "";
        }

        return secret
                .replace(" ", "")
                .replace("-", "")
                .replace("=", "")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private boolean constantTimeEquals(
            String expected,
            String actual
    ) {
        if (expected == null || actual == null) {
            return false;
        }

        if (expected.length() != actual.length()) {
            return false;
        }

        int result = 0;

        for (int i = 0; i < expected.length(); i++) {
            result |= expected.charAt(i) ^ actual.charAt(i);
        }

        return result == 0;
    }
}