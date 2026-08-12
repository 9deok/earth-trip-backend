package com.earthtrip;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;

final class HttpLogSanitizer {

    private static final String REDACTED = "[REDACTED]";
    private static final Set<String> REQUEST_HEADER_NAMES =
            Set.of(
                    "accept",
                    "authorization",
                    "contentlength",
                    "contenttype",
                    "idempotencykey",
                    "useragent",
                    "xoperationid",
                    "xrequestid",
                    "xtraceid");
    private static final Set<String> RESPONSE_HEADER_NAMES =
            Set.of("contentlength", "contenttype", "etag", "location", "retryafter", "xtraceid");
    private static final Set<String> SENSITIVE_NAMES =
            Set.of(
                    "authorization",
                    "proxyauthorization",
                    "cookie",
                    "setcookie",
                    "password",
                    "newpassword",
                    "currentpassword",
                    "passwordconfirmation",
                    "accesstoken",
                    "refreshtoken",
                    "idtoken",
                    "sessiontoken",
                    "authorizationcode",
                    "codeverifier",
                    "apikey",
                    "servicekey",
                    "privatekey",
                    "signingkey",
                    "encryptionkey",
                    "credential",
                    "pin");
    private static final Pattern SENSITIVE_PATH =
            Pattern.compile(
                    "(?i)(/api/v1/(?:invitations|shared-trips|storage/(?:uploads|downloads))/)"
                            + "([^/?#\\s]+)");
    private static final Pattern SENSITIVE_TEXT_PAIR =
            Pattern.compile(
                    "(?i)(\\b(?:password|newPassword|currentPassword|accessToken|refreshToken|"
                            + "idToken|sessionToken|authorizationCode|codeVerifier|apiKey|serviceKey|"
                            + "secret|signature|credential|token)\\b\\s*[=:]\\s*)([^&;,\\s]+)");

    private final ObjectMapper objectMapper;

    HttpLogSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String requestHeaders(HttpServletRequest request) {
        List<String> names = Collections.list(request.getHeaderNames());
        return headers(
                names, name -> Collections.list(request.getHeaders(name)), REQUEST_HEADER_NAMES);
    }

    String responseHeaders(HttpServletResponse response) {
        return headers(
                response.getHeaderNames(),
                name -> new ArrayList<>(response.getHeaders(name)),
                RESPONSE_HEADER_NAMES);
    }

    String query(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "{}";
        }
        Map<String, List<String>> values = new LinkedHashMap<>();
        for (String pair : rawQuery.split("&", -1)) {
            int separator = pair.indexOf('=');
            String name = decode(separator < 0 ? pair : pair.substring(0, separator));
            String value = separator < 0 ? "" : decode(pair.substring(separator + 1));
            values.computeIfAbsent(oneLine(name), ignored -> new ArrayList<>())
                    .add(isSensitiveName(name) ? REDACTED : oneLineWithSafePath(value));
        }
        return values.toString();
    }

    String path(String requestUri) {
        if (requestUri == null) {
            return "-";
        }
        return oneLine(SENSITIVE_PATH.matcher(requestUri).replaceAll("$1" + REDACTED));
    }

    String payload(
            byte[] content,
            String contentType,
            String characterEncoding,
            boolean overflowed,
            long totalOrDeclaredBytes) {
        if (content.length == 0) {
            return totalOrDeclaredBytes > 0
                    ? "[not captured; declaredBytes=" + totalOrDeclaredBytes + "]"
                    : "[empty]";
        }
        MediaType mediaType = mediaType(contentType);
        if (!isTextual(mediaType)) {
            return "[binary payload omitted; contentType="
                    + safeContentType(contentType)
                    + "; capturedBytes="
                    + content.length
                    + "; totalOrDeclaredBytes="
                    + totalOrDeclaredBytes
                    + "]";
        }
        if (overflowed) {
            return "[text payload truncated; contentType="
                    + safeContentType(contentType)
                    + "; capturedBytes="
                    + content.length
                    + "; totalOrDeclaredBytes="
                    + totalOrDeclaredBytes
                    + "]";
        }

        String text = new String(content, charset(mediaType, characterEncoding));
        if (isJson(mediaType)) {
            return json(text);
        }
        if (MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType)) {
            return query(text);
        }
        return redactText(text);
    }

    String oneLine(String value) {
        if (value == null) {
            return "-";
        }
        return value.replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private String headers(
            Iterable<String> names,
            Function<String, List<String>> valuesProvider,
            Set<String> includedNames) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (String name : names) {
            if (!includedNames.contains(normalizeName(name))) {
                continue;
            }
            List<String> values =
                    isSensitiveName(name)
                            ? List.of(REDACTED)
                            : valuesProvider.apply(name).stream()
                                    .map(this::oneLineWithSafePath)
                                    .toList();
            headers.put(oneLine(name), values);
        }
        if (headers.isEmpty()) {
            return "[none]";
        }
        return headers.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + String.join(", ", entry.getValue()))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String json(String text) {
        try {
            JsonNode root = objectMapper.readTree(text);
            redactJson(root);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            return "[unparseable JSON payload omitted]";
        }
    }

    private void redactJson(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);
            for (String fieldName : fieldNames) {
                JsonNode value = objectNode.get(fieldName);
                if (isSensitiveName(fieldName)) {
                    objectNode.set(fieldName, TextNode.valueOf(REDACTED));
                } else if (value.isTextual()) {
                    objectNode.set(
                            fieldName, TextNode.valueOf(oneLineWithSafePath(value.textValue())));
                } else {
                    redactJson(value);
                }
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode value : node) {
                redactJson(value);
            }
        }
    }

    private String redactText(String text) {
        String withRedactedPairs = SENSITIVE_TEXT_PAIR.matcher(text).replaceAll("$1" + REDACTED);
        return oneLineWithSafePath(withRedactedPairs);
    }

    private String oneLineWithSafePath(String value) {
        if (value == null) {
            return "null";
        }
        return oneLine(SENSITIVE_PATH.matcher(value).replaceAll("$1" + REDACTED));
    }

    private static boolean isSensitiveName(String name) {
        String normalized = normalizeName(name);
        return SENSITIVE_NAMES.contains(normalized)
                || normalized.endsWith("password")
                || normalized.endsWith("token")
                || normalized.endsWith("secret")
                || normalized.endsWith("signature")
                || normalized.endsWith("credential")
                || normalized.endsWith("apikey")
                || normalized.endsWith("servicekey");
    }

    private static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static MediaType mediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (RuntimeException ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private static boolean isTextual(MediaType mediaType) {
        String subtype = mediaType.getSubtype().toLowerCase(Locale.ROOT);
        return "text".equals(mediaType.getType())
                || isJson(mediaType)
                || subtype.equals("xml")
                || subtype.endsWith("+xml")
                || MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType);
    }

    private static boolean isJson(MediaType mediaType) {
        String subtype = mediaType.getSubtype().toLowerCase(Locale.ROOT);
        return subtype.equals("json") || subtype.endsWith("+json");
    }

    private static Charset charset(MediaType mediaType, String characterEncoding) {
        if (mediaType.getCharset() != null) {
            return mediaType.getCharset();
        }
        if (isJson(mediaType)) {
            return StandardCharsets.UTF_8;
        }
        if (characterEncoding != null && !characterEncoding.isBlank()) {
            try {
                return Charset.forName(characterEncoding);
            } catch (RuntimeException ignored) {
                // Fall through to UTF-8, which is the JSON and application default here.
            }
        }
        return StandardCharsets.UTF_8;
    }

    private static String safeContentType(String contentType) {
        return contentType == null ? "unspecified" : contentType.replaceAll("[\\r\\n]", "");
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return value;
        }
    }
}
