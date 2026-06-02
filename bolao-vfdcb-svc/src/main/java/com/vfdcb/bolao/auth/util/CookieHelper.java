package com.vfdcb.bolao.auth.util;

import com.vfdcb.bolao.auth.exception.AuthException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class CookieHelper {

    private static final String HMAC_ALGO = "HmacSHA256";

    public String sign(String data, String secret) {
        try {
            String dataB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(data.getBytes(StandardCharsets.UTF_8));
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String signatureB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
            return dataB64 + "." + signatureB64;
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign cookie", e);
        }
    }

    public String verify(String signedValue, String secret) {
        String[] parts = signedValue.split("\\.");
        if (parts.length != 2) {
            throw new AuthException("invalid cookie format");
        }

        try {
            String dataB64 = parts[0];
            String signatureB64 = parts[1];

            byte[] data = Base64.getUrlDecoder().decode(dataB64);
            byte[] signature = Base64.getUrlDecoder().decode(signatureB64);

            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
            mac.init(secretKeySpec);
            byte[] expectedSignature = mac.doFinal(data);

            if (!MessageDigest.isEqual(signature, expectedSignature)) {
                throw new AuthException("invalid signature");
            }

            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new AuthException("invalid signature or format");
        }
    }
}
