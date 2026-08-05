package com.earthtrip.platform.adapter.out.provider;

import com.earthtrip.platform.application.port.in.ProviderProxyUseCase;
import com.earthtrip.platform.application.port.out.LinkPreviewProviderPort;
import com.earthtrip.sharedkernel.error.EarthTripException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class SafeLinkPreviewProviderAdapter implements LinkPreviewProviderPort {

    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_HTML_BYTES = 1_048_576;

    private final HttpClient httpClient;
    private final Clock clock;

    @Autowired
    SafeLinkPreviewProviderAdapter(Clock clock) {
        this(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build(), clock);
    }

    SafeLinkPreviewProviderAdapter(HttpClient httpClient, Clock clock) {
        this.httpClient = httpClient;
        this.clock = clock;
    }

    @Override
    public ProviderProxyUseCase.LinkPreviewResult preview(String url) {
        URI current = validatedUri(url);
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            validatePublicHost(current);
            HttpResponse<InputStream> response = request(current);
            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                close(response.body());
                String location = response.headers().firstValue("Location").orElseThrow(() ->
                    new EarthTripException(
                        "INVALID_PREVIEW_REDIRECT",
                        502,
                        "링크 미리보기 응답의 이동 주소가 없습니다."
                    )
                );
                current = validatedUri(current.resolve(location).toString());
                continue;
            }
            if (status < 200 || status >= 300) {
                close(response.body());
                throw new EarthTripException(
                    "LINK_PREVIEW_REQUEST_REJECTED",
                    502,
                    "대상 사이트가 링크 미리보기 요청을 거절했습니다."
                );
            }
            String contentType = response.headers().firstValue("Content-Type")
                .orElse("").toLowerCase(Locale.ROOT);
            if (!contentType.startsWith("text/html")
                && !contentType.startsWith("application/xhtml+xml")) {
                close(response.body());
                throw EarthTripException.badRequest(
                    "UNSUPPORTED_PREVIEW_CONTENT",
                    "HTML 문서만 링크 미리보기를 만들 수 있습니다."
                );
            }
            byte[] html = readBounded(response.body());
            return parse(current, html);
        }
        throw new EarthTripException(
            "TOO_MANY_PREVIEW_REDIRECTS",
            502,
            "링크 이동 횟수가 허용 범위를 초과했습니다."
        );
    }

    private HttpResponse<InputStream> request(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(8))
            .header("Accept", "text/html,application/xhtml+xml")
            .header("User-Agent", "EarthTrip-LinkPreview/1.0")
            .GET()
            .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (IOException exception) {
            throw unavailable();
        }
    }

    private ProviderProxyUseCase.LinkPreviewResult parse(URI responseUri, byte[] html) {
        try {
            Document document = Jsoup.parse(
                new ByteArrayInputStream(html),
                null,
                responseUri.toString()
            );
            String canonical = firstContent(document,
                "meta[property=og:url]", "content",
                "link[rel=canonical]", "href"
            );
            String title = firstContent(document,
                "meta[property=og:title]", "content",
                "title", "text"
            );
            String description = firstContent(document,
                "meta[property=og:description]", "content",
                "meta[name=description]", "content"
            );
            String image = firstContent(document,
                "meta[property=og:image]", "content",
                "meta[name=twitter:image]", "content"
            );
            String siteName = firstContent(document,
                "meta[property=og:site_name]", "content",
                "meta[name=application-name]", "content"
            );
            return new ProviderProxyUseCase.LinkPreviewResult(
                normalizedResultUrl(responseUri, canonical, true),
                limit(title, 300),
                limit(description, 1_000),
                normalizedResultUrl(responseUri, image, false),
                siteName == null ? responseUri.getHost() : limit(siteName, 160),
                "HTML_OPEN_GRAPH",
                clock.instant()
            );
        } catch (IOException exception) {
            throw new EarthTripException(
                "INVALID_LINK_PREVIEW_DOCUMENT",
                502,
                "링크의 HTML 문서를 해석할 수 없습니다."
            );
        }
    }

    private static String firstContent(
        Document document,
        String firstSelector,
        String firstAttribute,
        String secondSelector,
        String secondAttribute
    ) {
        String first = content(document.selectFirst(firstSelector), firstAttribute);
        return first == null
            ? content(document.selectFirst(secondSelector), secondAttribute)
            : first;
    }

    private static String content(Element element, String attribute) {
        if (element == null) {
            return null;
        }
        String value = "text".equals(attribute) ? element.text() : element.attr(attribute);
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String normalizedResultUrl(URI base, String value, boolean fallbackToBase) {
        if (value == null || value.isBlank()) {
            return fallbackToBase ? base.toString() : null;
        }
        try {
            URI resolved = base.resolve(value).normalize();
            String scheme = resolved.getScheme() == null
                ? ""
                : resolved.getScheme().toLowerCase(Locale.ROOT);
            return ("http".equals(scheme) || "https".equals(scheme))
                ? resolved.toString()
                : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static URI validatedUri(String value) {
        try {
            URI uri = new URI(value).normalize();
            String scheme = uri.getScheme() == null
                ? ""
                : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!("http".equals(scheme) || "https".equals(scheme))
                || uri.getHost() == null || uri.getUserInfo() != null) {
                throw unsafe();
            }
            return uri;
        } catch (URISyntaxException | NullPointerException exception) {
            throw EarthTripException.badRequest("INVALID_PREVIEW_URL", "URL 형식을 확인해 주세요.");
        }
    }

    private static void validatePublicHost(URI uri) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(uri.getHost());
        } catch (UnknownHostException exception) {
            throw new EarthTripException(
                "PREVIEW_HOST_NOT_FOUND",
                502,
                "링크의 호스트를 찾을 수 없습니다."
            );
        }
        if (addresses.length == 0) {
            throw unsafe();
        }
        for (InetAddress address : addresses) {
            if (!isPublic(address)) {
                throw unsafe();
            }
        }
    }

    private static boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
            || address.isLinkLocalAddress() || address.isSiteLocalAddress()
            || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            return first != 0
                && !(first == 100 && second >= 64 && second <= 127)
                && !(first == 192 && second == 0)
                && first < 224;
        }
        if (address instanceof Inet6Address) {
            return (bytes[0] & 0xFE) != 0xFC;
        }
        return false;
    }

    private static byte[] readBounded(InputStream input) {
        try (input) {
            byte[] result = input.readNBytes(MAX_HTML_BYTES + 1);
            if (result.length > MAX_HTML_BYTES) {
                throw EarthTripException.badRequest(
                    "PREVIEW_DOCUMENT_TOO_LARGE",
                    "링크 미리보기 문서가 너무 큽니다."
                );
            }
            return result;
        } catch (IOException exception) {
            throw unavailable();
        }
    }

    private static void close(InputStream input) {
        try {
            input.close();
        } catch (IOException ignored) {
            // 응답 본문 정리 실패는 원래 HTTP 상태 처리보다 우선하지 않는다.
        }
    }

    private static String limit(String value, int maximumLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maximumLength ? value : value.substring(0, maximumLength);
    }

    private static EarthTripException unsafe() {
        return EarthTripException.badRequest(
            "UNSAFE_PREVIEW_URL",
            "공개 인터넷의 HTTP(S) URL만 미리 볼 수 있습니다."
        );
    }

    private static EarthTripException unavailable() {
        return EarthTripException.unavailable(
            "LINK_PREVIEW_PROVIDER_UNAVAILABLE",
            "링크 미리보기 대상에 연결할 수 없습니다."
        );
    }
}
