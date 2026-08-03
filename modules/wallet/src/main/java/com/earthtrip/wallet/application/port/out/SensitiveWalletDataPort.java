package com.earthtrip.wallet.application.port.out;

public interface SensitiveWalletDataPort {

    Object protect(String fieldName, Object value);

    Object reveal(String fieldName, Object storedValue);

    boolean isProtected(Object storedValue);
}
