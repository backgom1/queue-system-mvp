package learn.queuesystem.infra.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EncryptQueueTokenProvider {

    private static final int TOKEN_BYTES = 24; // 192-bit entropy

    private final SecureRandom secureRandom = new SecureRandom();

    public String issue() {
        byte[] buffer = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }
}
