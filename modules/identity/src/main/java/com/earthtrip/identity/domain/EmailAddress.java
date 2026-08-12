package com.earthtrip.identity.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record EmailAddress(String value) {

    private static final Pattern PATTERN =
            Pattern.compile(
                    "^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+$",
                    Pattern.CASE_INSENSITIVE);

    public EmailAddress {
        Objects.requireNonNull(value, "이메일은 필수입니다.");
        value = value.strip().toLowerCase(Locale.ROOT);
        if (value.length() > 320 || !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("올바른 이메일을 입력해 주세요.");
        }
    }
}
