package com.earthtrip.platform.application.service.file;

import com.earthtrip.sharedkernel.error.EarthTripException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class FilePolicy {
    private static final long MAX_FILE_SIZE_BYTES = 25L * 1024L * 1024L;
    private static final Set<String> ALLOWED_MIME_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp",
                    "image/heic",
                    "image/heif",
                    "application/pdf",
                    "text/plain");
    private static final Set<String> RESOURCE_TYPES =
            Set.of(
                    "RESERVATION",
                    "RESEARCH_SOURCE",
                    "EXPENSE",
                    "DECISION",
                    "PREPARATION_TASK",
                    "PACKING_ITEM",
                    "DIARY_ENTRY",
                    "SUPPORT_REQUEST");
    private static final Set<String> VISIBILITIES = Set.of("PRIVATE", "TRIP");

    private FilePolicy() {}

    static String fileName(String fileName) {
        if (fileName == null) {
            throw EarthTripException.badRequest("FILE_NAME_REQUIRED", "파일 이름이 필요합니다.");
        }
        String normalized = fileName.strip();
        if (normalized.isEmpty()
                || normalized.length() > 255
                || normalized.indexOf('/') >= 0
                || normalized.indexOf('\\') >= 0
                || normalized.chars().anyMatch(Character::isISOControl)) {
            throw EarthTripException.badRequest("INVALID_FILE_NAME", "안전하지 않은 파일 이름입니다.");
        }
        return normalized;
    }

    static String mimeType(String mimeType) {
        String normalized = mimeType == null ? "" : mimeType.strip().toLowerCase(Locale.ROOT);
        if (!ALLOWED_MIME_TYPES.contains(normalized)) {
            throw new EarthTripException(
                    "UNSUPPORTED_FILE_TYPE",
                    415,
                    "지원하지 않는 파일 형식입니다.",
                    Map.of("allowedMimeTypes", ALLOWED_MIME_TYPES));
        }
        return normalized;
    }

    static void size(long sizeBytes) {
        if (sizeBytes <= 0 || sizeBytes > MAX_FILE_SIZE_BYTES) {
            throw new EarthTripException(
                    "INVALID_FILE_SIZE",
                    413,
                    "파일 크기는 1바이트 이상 25MB 이하여야 합니다.",
                    Map.of("maximumBytes", MAX_FILE_SIZE_BYTES));
        }
    }

    static String checksum(String checksum) {
        String normalized = checksum == null ? "" : checksum.strip().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw EarthTripException.badRequest(
                    "INVALID_CHECKSUM", "SHA-256 체크섬은 64자리 16진수여야 합니다.");
        }
        return normalized;
    }

    static String resourceType(String resourceType) {
        String normalized =
                resourceType == null ? "" : resourceType.strip().toUpperCase(Locale.ROOT);
        if (!RESOURCE_TYPES.contains(normalized)) {
            throw EarthTripException.badRequest(
                    "INVALID_FILE_RESOURCE_TYPE", "지원하지 않는 파일 연결 대상입니다.");
        }
        return normalized;
    }

    static String visibility(String visibility) {
        String normalized =
                visibility == null ? "TRIP" : visibility.strip().toUpperCase(Locale.ROOT);
        if (!VISIBILITIES.contains(normalized)) {
            throw EarthTripException.badRequest(
                    "INVALID_FILE_VISIBILITY", "파일 공개 범위는 PRIVATE 또는 TRIP이어야 합니다.");
        }
        return normalized;
    }
}
