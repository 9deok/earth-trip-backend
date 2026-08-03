package com.earthtrip.sharedkernel.error;

import java.util.Map;
import java.util.Objects;

@SuppressWarnings("serial")
public final class EarthTripException extends RuntimeException {

    private final String code;
    private final int httpStatus;
    private final Map<String, Object> properties;

    public EarthTripException(String code, int httpStatus, String message) {
        this(code, httpStatus, message, Map.of());
    }

    public EarthTripException(
        String code,
        int httpStatus,
        String message,
        Map<String, Object> properties
    ) {
        super(Objects.requireNonNull(message, "오류 메시지는 필수입니다."));
        this.code = requireText(code, "오류 코드는 필수입니다.");
        if (httpStatus < 400 || httpStatus > 599) {
            throw new IllegalArgumentException("HTTP 오류 상태는 400에서 599 사이여야 합니다.");
        }
        this.httpStatus = httpStatus;
        this.properties = Map.copyOf(Objects.requireNonNull(properties));
    }

    public String code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public Map<String, Object> properties() {
        return properties;
    }

    public static EarthTripException badRequest(String code, String message) {
        return new EarthTripException(code, 400, message);
    }

    public static EarthTripException unauthorized(String code, String message) {
        return new EarthTripException(code, 401, message);
    }

    public static EarthTripException forbidden(String code, String message) {
        return new EarthTripException(code, 403, message);
    }

    public static EarthTripException notFound(String code, String message) {
        return new EarthTripException(code, 404, message);
    }

    public static EarthTripException conflict(String code, String message) {
        return new EarthTripException(code, 409, message);
    }

    public static EarthTripException unavailable(String code, String message) {
        return new EarthTripException(code, 503, message);
    }

    private static String requireText(String value, String message) {
        Objects.requireNonNull(value, message);
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }
}
