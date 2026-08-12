package com.earthtrip.platform.application.port.out;

public interface ContentDigestPort {

    String sha256(byte[] content);
}
