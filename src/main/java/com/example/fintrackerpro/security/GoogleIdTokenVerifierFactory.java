package com.example.fintrackerpro.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GoogleIdTokenVerifierFactory {

    private final List<String> audiences;
    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokenVerifierFactory(
            @Value("${spring.security.oauth2.client.registration.google.client-id:}") String springClientId,
            @Value("${GOOGLE_CLIENT_ID:}") String googleClientId,
            @Value("${GOOGLE_CLIENT_IDS:}") String extraClientIds
    ) {
        this.audiences = buildAudienceList(springClientId, googleClientId, extraClientIds);
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(audiences)
                .build();

        if (audiences.isEmpty()) {
            log.error(
                    "Google OAuth is NOT configured: set GOOGLE_CLIENT_ID on the server " +
                            "(must match REACT_APP_GOOGLE_CLIENT_ID on the frontend)"
            );
        } else {
            log.info(
                    "Google OAuth verifier ready, {} audience(s), primary suffix: …{}",
                    audiences.size(),
                    tail(audiences.get(0))
            );
        }
    }

    public boolean isConfigured() {
        return !audiences.isEmpty();
    }

    public List<String> audienceSuffixes() {
        return audiences.stream().map(GoogleIdTokenVerifierFactory::tail).toList();
    }

    public GoogleIdTokenVerifier verifier() {
        return verifier;
    }

    private static List<String> buildAudienceList(String springClientId, String googleClientId, String extra) {
        List<String> raw = new ArrayList<>();
        raw.add(springClientId);
        raw.add(googleClientId);
        if (StringUtils.hasText(extra)) {
            raw.addAll(Arrays.asList(extra.split(",")));
        }
        List<String> valid = raw.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(GoogleIdTokenVerifierFactory::isValidGoogleClientId)
                .distinct()
                .collect(Collectors.toList());

        long rejected = raw.stream().filter(StringUtils::hasText).count() - valid.size();
        if (rejected > 0) {
            log.error(
                    "Ignored invalid GOOGLE_CLIENT_ID value(s). On Render set the real client id " +
                            "from Google Cloud (…apps.googleusercontent.com), not the placeholder text GOOGLE_CLIENT_ID"
            );
        }
        return valid;
    }

    /** Отсекает пустые значения и типичную ошибку: в Render вписали имя переменной вместо значения. */
    static boolean isValidGoogleClientId(String id) {
        if (!StringUtils.hasText(id)) return false;
        String s = id.trim();
        if ("GOOGLE_CLIENT_ID".equalsIgnoreCase(s)) return false;
        if (s.startsWith("${") && s.endsWith("}")) return false;
        return s.endsWith(".apps.googleusercontent.com") && s.length() > 30;
    }

    private static String tail(String clientId) {
        if (!StringUtils.hasText(clientId)) return "?";
        String s = clientId.trim();
        return s.length() <= 24 ? s : s.substring(s.length() - 24);
    }
}
