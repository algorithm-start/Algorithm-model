package com.recplatform.audit.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Masks sensitive data in audit log detail JSON strings.
 * Patterns for: passwords, authorization headers, API keys.
 */
@Component
public class SensitiveDataMasker {

    private final List<Pattern> patterns = new ArrayList<>();

    public SensitiveDataMasker() {
        // Password fields (e.g., "password":"secret123")
        patterns.add(Pattern.compile("(\"(?:password|passwd|pwd)\"\\s*:\\s*\")([^\"]*)(\")",
                Pattern.CASE_INSENSITIVE));
        // Authorization headers (e.g., "authorization":"Bearer xxx")
        patterns.add(Pattern.compile("(\"(?:authorization|token|access_token|refresh_token)\"\\s*:\\s*\")([^\"]*)(\")",
                Pattern.CASE_INSENSITIVE));
        // API keys (e.g., "api_key":"xxx", "secret_key":"xxx")
        patterns.add(Pattern.compile("(\"(?:api_?[kK]ey|secret_?[kK]ey|private_?[kK]ey|apiKey|secretKey)\"\\s*:\\s*\")([^\"]*)(\")",
                Pattern.CASE_INSENSITIVE));
    }

    /**
     * Mask sensitive fields in a JSON string.
     * Replaces sensitive values with "****".
     *
     * @param json the original JSON string
     * @return the masked JSON string
     */
    public String maskAuditDetail(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        String result = json;
        for (Pattern pattern : patterns) {
            result = pattern.matcher(result).replaceAll("$1****$3");
        }
        return result;
    }
}
