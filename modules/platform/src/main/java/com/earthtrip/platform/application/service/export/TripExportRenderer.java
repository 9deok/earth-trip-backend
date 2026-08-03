package com.earthtrip.platform.application.service.export;

import com.earthtrip.expense.api.TripExpenseView;
import com.earthtrip.planning.api.TripPlanningView;
import com.earthtrip.trip.api.TripStructureView;
import com.earthtrip.wallet.api.TripWalletView;
import com.earthtrip.platform.application.service.export.UnicodePdfRenderer.TextLine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class TripExportRenderer {

    private static final DateTimeFormatter ICS_TIME = DateTimeFormatter
        .ofPattern("yyyyMMdd'T'HHmmss'Z'")
        .withZone(ZoneOffset.UTC);

    private final TripStructureView structures;
    private final TripPlanningView planning;
    private final TripWalletView wallet;
    private final TripExpenseView expenses;
    private final ObjectMapper json;
    private final UnicodePdfRenderer pdfRenderer;

    TripExportRenderer(
        TripStructureView structures,
        TripPlanningView planning,
        TripWalletView wallet,
        TripExpenseView expenses,
        ObjectMapper json,
        UnicodePdfRenderer pdfRenderer
    ) {
        this.structures = structures;
        this.planning = planning;
        this.wallet = wallet;
        this.expenses = expenses;
        this.json = json;
        this.pdfRenderer = pdfRenderer;
    }

    RenderedArtifact render(
        UUID tripId,
        UUID actorUserId,
        String format,
        Set<String> scopes
    ) {
        ExportBundle bundle = bundle(tripId, actorUserId, scopes);
        return switch (format) {
            case "JSON" -> artifact(bundle, "json", "application/json", json(bundle));
            case "CSV" -> artifact(bundle, "csv", "text/csv; charset=UTF-8", csv(bundle));
            case "ICS" -> artifact(bundle, "ics", "text/calendar; charset=UTF-8", ics(bundle));
            case "KML" -> artifact(bundle, "kml", "application/vnd.google-earth.kml+xml", kml(bundle));
            case "PDF" -> artifact(bundle, "pdf", "application/pdf", pdf(bundle));
            default -> throw new IllegalArgumentException("지원하지 않는 내보내기 형식입니다.");
        };
    }

    private ExportBundle bundle(UUID tripId, UUID actorUserId, Set<String> scopes) {
        TripStructureView.StructureSnapshot structure = structures.snapshot(tripId, actorUserId);
        List<TripPlanningView.SearchEntry> planningEntries = scopes.contains("PLANNING")
            ? planning.searchEntries(tripId, actorUserId)
            : List.of();
        TripWalletView.WalletSnapshot walletSnapshot = scopes.contains("WALLET")
            ? wallet.snapshot(tripId, actorUserId)
            : null;
        List<TripExpenseView.Entry> expenseEntries = scopes.contains("EXPENSE")
            ? expenses.searchEntries(tripId, actorUserId)
            : List.of();
        return new ExportBundle(
            structure.trip(), scopes.contains("STRUCTURE") ? structure.segments() : List.of(),
            planningEntries, walletSnapshot, expenseEntries
        );
    }

    private byte[] json(ExportBundle bundle) {
        try {
            return json.writerWithDefaultPrettyPrinter().writeValueAsBytes(bundle);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("JSON 내보내기를 생성할 수 없습니다.", exception);
        }
    }

    private static byte[] csv(ExportBundle bundle) {
        StringBuilder value = new StringBuilder("\uFEFFsection,type,title,date,amount,currency,status\r\n");
        for (TripPlanningView.SearchEntry entry : bundle.planning()) {
            value.append("planning,").append(csvValue(entry.type())).append(',')
                .append(csvValue(title(entry.payload()))).append(',')
                .append(csvValue(entry.localDate() == null ? "" : entry.localDate().toString()))
                .append(",,,").append(csvValue(entry.status())).append("\r\n");
        }
        for (TripExpenseView.Entry entry : bundle.expenses()) {
            value.append("expense,").append(csvValue(entry.categoryCode())).append(',')
                .append(csvValue(entry.title())).append(',')
                .append(csvValue(entry.occurredAt().toString())).append(',')
                .append(entry.amountMinor()).append(',').append(csvValue(entry.currency()))
                .append(',').append(csvValue(entry.status())).append("\r\n");
        }
        return value.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] ics(ExportBundle bundle) {
        StringBuilder value = new StringBuilder(
            "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//EarthTrip//Trip Export//KO\r\n"
        );
        for (TripPlanningView.SearchEntry entry : bundle.planning()) {
            if (!entry.type().equals("SCHEDULE_ITEM")) {
                continue;
            }
            Instant start = instant(entry.payload(), "startAt", "startsAt");
            Instant end = instant(entry.payload(), "endAt", "endsAt");
            if (start == null) {
                continue;
            }
            value.append("BEGIN:VEVENT\r\nUID:").append(entry.id()).append("@earthtrip\r\n")
                .append("DTSTAMP:").append(ICS_TIME.format(Instant.now())).append("\r\n")
                .append("DTSTART:").append(ICS_TIME.format(start)).append("\r\n");
            if (end != null) {
                value.append("DTEND:").append(ICS_TIME.format(end)).append("\r\n");
            }
            value.append("SUMMARY:").append(icsEscape(title(entry.payload())))
                .append("\r\nEND:VEVENT\r\n");
        }
        value.append("END:VCALENDAR\r\n");
        return value.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] kml(ExportBundle bundle) {
        StringBuilder value = new StringBuilder(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document>"
        );
        value.append("<name>").append(xml(bundle.trip().title())).append("</name>");
        for (TripStructureView.Segment segment : bundle.segments()) {
            if (segment.latitude() == null || segment.longitude() == null) {
                continue;
            }
            value.append("<Placemark><name>").append(xml(
                segment.cityName() == null ? segment.type() : segment.cityName()
            )).append("</name><Point><coordinates>")
                .append(segment.longitude()).append(',').append(segment.latitude())
                .append(",0</coordinates></Point></Placemark>");
        }
        for (TripPlanningView.SearchEntry entry : bundle.planning()) {
            Double latitude = number(entry.payload(), "latitude", "lat");
            Double longitude = number(entry.payload(), "longitude", "lng", "lon");
            if (latitude == null || longitude == null) {
                continue;
            }
            value.append("<Placemark><name>").append(xml(title(entry.payload())))
                .append("</name><Point><coordinates>").append(longitude).append(',')
                .append(latitude).append(",0</coordinates></Point></Placemark>");
        }
        value.append("</Document></kml>");
        return value.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] pdf(ExportBundle bundle) {
        List<TextLine> lines = new java.util.ArrayList<>();
        lines.add(TextLine.title(bundle.trip().title()));
        lines.add(TextLine.subtitle("Earth Trip 여행 일정"));
        lines.add(TextLine.caption(
            "기간  " + value(bundle.trip().startDate()) + " — " + value(bundle.trip().endDate())
        ));
        lines.add(TextLine.caption(
            "시간대  " + value(bundle.trip().timeZone()) + "  ·  기본 통화  "
                + value(bundle.trip().defaultCurrency())
        ));
        lines.add(TextLine.spacer());
        lines.add(TextLine.body(
            "도시·이동 " + bundle.segments().size() + "개  ·  계획 "
                + bundle.planning().size() + "개  ·  지출 " + bundle.expenses().size() + "건"
        ));
        appendSegments(lines, bundle.segments());
        appendPlanning(lines, bundle.planning());
        appendWallet(lines, bundle.wallet());
        appendExpenses(lines, bundle.expenses());
        return pdfRenderer.render(bundle.trip().title(), lines);
    }

    private static void appendSegments(
        List<TextLine> lines,
        List<TripStructureView.Segment> segments
    ) {
        if (segments.isEmpty()) {
            return;
        }
        lines.add(TextLine.section("전체 여행 동선"));
        int number = 1;
        for (TripStructureView.Segment segment : segments) {
            String name = segment.cityName() == null ? segment.type() : segment.cityName();
            String period = segment.startDate() == null && segment.endDate() == null
                ? "" : "  ·  " + value(segment.startDate()) + " — " + value(segment.endDate());
            lines.add(TextLine.body(number++ + ".  " + name + period));
            if (segment.accommodationName() != null) {
                lines.add(TextLine.caption("    숙소  " + segment.accommodationName()));
            }
            if (segment.transportMode() != null) {
                lines.add(TextLine.caption("    이동  " + segment.transportMode()));
            }
        }
    }

    private static void appendPlanning(
        List<TextLine> lines,
        List<TripPlanningView.SearchEntry> entries
    ) {
        if (entries.isEmpty()) {
            return;
        }
        lines.add(TextLine.section("세부 일정과 후보"));
        for (TripPlanningView.SearchEntry entry : entries) {
            String date = entry.localDate() == null ? "날짜 미정" : entry.localDate().toString();
            lines.add(TextLine.body(date + "  ·  " + title(entry.payload())));
            lines.add(TextLine.caption(
                "    " + entry.type() + "  ·  상태 " + value(entry.status())
            ));
        }
    }

    private static void appendWallet(
        List<TextLine> lines,
        TripWalletView.WalletSnapshot snapshot
    ) {
        if (snapshot == null) {
            return;
        }
        lines.add(TextLine.section("예약과 여행 지갑"));
        lines.add(TextLine.body(
            "예약 " + snapshot.reservations().size() + "건  ·  티켓 "
                + snapshot.tickets().size() + "건  ·  오프라인 준비 "
                + snapshot.offlineReadyCount() + "건  ·  확인 필요 "
                + snapshot.actionRequiredCount() + "건"
        ));
        for (TripWalletView.Entry entry : snapshot.reservations()) {
            lines.add(TextLine.caption(
                "    " + title(entry.payload()) + "  ·  " + value(entry.status())
            ));
        }
        for (TripWalletView.Entry entry : snapshot.tickets()) {
            lines.add(TextLine.caption(
                "    " + title(entry.payload()) + "  ·  " + value(entry.status())
            ));
        }
    }

    private static void appendExpenses(
        List<TextLine> lines,
        List<TripExpenseView.Entry> entries
    ) {
        if (entries.isEmpty()) {
            return;
        }
        lines.add(TextLine.section("지출 기록"));
        for (TripExpenseView.Entry entry : entries) {
            lines.add(TextLine.body(
                entry.occurredAt().toString() + "  ·  " + entry.title() + "  ·  "
                    + amount(entry.amountMinor(), entry.currency())
            ));
            lines.add(TextLine.caption(
                "    " + value(entry.categoryCode()) + "  ·  상태 " + value(entry.status())
            ));
        }
    }

    private static String amount(long amountMinor, String currencyCode) {
        try {
            Currency currency = Currency.getInstance(currencyCode);
            int fractionDigits = Math.max(currency.getDefaultFractionDigits(), 0);
            BigDecimal amount = BigDecimal.valueOf(amountMinor, fractionDigits)
                .setScale(fractionDigits, RoundingMode.UNNECESSARY);
            return amount.toPlainString() + " " + currencyCode;
        } catch (IllegalArgumentException exception) {
            return amountMinor + " " + value(currencyCode);
        }
    }

    private static String value(Object value) {
        return value == null || String.valueOf(value).isBlank() ? "미정" : String.valueOf(value);
    }

    private static RenderedArtifact artifact(
        ExportBundle bundle,
        String extension,
        String mimeType,
        byte[] content
    ) {
        String base = bundle.trip().title().replaceAll("[^\\p{L}\\p{N}._-]+", "-");
        if (base.isBlank()) {
            base = "earth-trip";
        }
        return new RenderedArtifact(base + "." + extension, mimeType, content);
    }

    private static String title(Map<String, Object> payload) {
        for (String key : List.of("title", "name", "placeName")) {
            Object value = payload.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return "Untitled";
    }

    private static Instant instant(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value == null) {
                continue;
            }
            try {
                return Instant.parse(String.valueOf(value));
            } catch (RuntimeException ignored) {
                try {
                    return OffsetDateTime.parse(String.valueOf(value)).toInstant();
                } catch (RuntimeException alsoIgnored) {
                    // Try the next accepted field.
                }
            }
        }
        return null;
    }

    private static Double number(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            if (value != null) {
                try {
                    return Double.parseDouble(String.valueOf(value));
                } catch (NumberFormatException ignored) {
                    // Try the next accepted field.
                }
            }
        }
        return null;
    }

    private static String csvValue(String value) {
        return '"' + (value == null ? "" : value.replace("\"", "\"\"")) + '"';
    }

    private static String icsEscape(String value) {
        return value.replace("\\", "\\\\").replace(";", "\\;")
            .replace(",", "\\,").replace("\n", "\\n");
    }

    private static String xml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;");
    }

    record ExportBundle(
        TripStructureView.Trip trip,
        List<TripStructureView.Segment> segments,
        List<TripPlanningView.SearchEntry> planning,
        TripWalletView.WalletSnapshot wallet,
        List<TripExpenseView.Entry> expenses
    ) { }

    record RenderedArtifact(String fileName, String mimeType, byte[] content) { }
}
