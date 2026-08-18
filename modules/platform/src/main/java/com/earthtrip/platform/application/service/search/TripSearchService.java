package com.earthtrip.platform.application.service.search;

import com.earthtrip.expense.api.TripExpenseView;
import com.earthtrip.planning.api.TripPlanningView;
import com.earthtrip.platform.application.port.in.TripSearchUseCase;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripStructureView;
import com.earthtrip.wallet.api.TripWalletView;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class TripSearchService implements TripSearchUseCase {

    private static final Set<String> DOMAINS =
            Set.of("STRUCTURE", "PLANNING", "RESERVATION", "WALLET", "EXPENSE");

    private final TripStructureView structure;
    private final TripPlanningView planning;
    private final TripWalletView wallet;
    private final TripExpenseView expenses;

    TripSearchService(
            TripStructureView structure,
            TripPlanningView planning,
            TripWalletView wallet,
            TripExpenseView expenses) {
        this.structure = structure;
        this.planning = planning;
        this.wallet = wallet;
        this.expenses = expenses;
    }

    @Override
    public SearchResult search(
            UUID tripId, UUID actorUserId, String query, List<String> types, Integer limit) {
        String normalizedQuery = query(query);
        Set<String> domains = domains(types);
        int pageSize = limit(limit);
        List<SearchItem> candidates = new ArrayList<>();
        if (domains.contains("STRUCTURE")) {
            addStructure(candidates, tripId, actorUserId, normalizedQuery);
        }
        if (domains.contains("PLANNING")) {
            addPlanning(candidates, tripId, actorUserId, normalizedQuery);
        }
        TripWalletView.WalletSnapshot walletSnapshot = null;
        if (domains.contains("RESERVATION") || domains.contains("WALLET")) {
            walletSnapshot = wallet.snapshot(tripId, actorUserId);
        }
        if (domains.contains("RESERVATION")) {
            addWallet(candidates, walletSnapshot.reservations(), "RESERVATION", normalizedQuery);
        }
        if (domains.contains("WALLET")) {
            addWallet(candidates, walletSnapshot.tickets(), "WALLET", normalizedQuery);
        }
        if (domains.contains("EXPENSE")) {
            addExpenses(candidates, tripId, actorUserId, normalizedQuery);
        }
        List<SearchItem> sorted =
                candidates.stream()
                        .sorted(
                                Comparator.comparingDouble(SearchItem::score)
                                        .reversed()
                                        .thenComparing(SearchItem::title))
                        .toList();
        return new SearchResult(
                query.strip(), sorted.stream().limit(pageSize).toList(), sorted.size());
    }

    private void addStructure(
            List<SearchItem> output, UUID tripId, UUID actorUserId, String query) {
        TripStructureView.StructureSnapshot snapshot = structure.snapshot(tripId, actorUserId);
        add(
                output,
                snapshot.trip().tripId(),
                "STRUCTURE",
                "TRIP",
                snapshot.trip().title(),
                null,
                query,
                Map.of());
        for (TripStructureView.Segment segment : snapshot.segments()) {
            String title = segment.cityName() == null ? segment.type() : segment.cityName();
            Map<String, Object> metadata = new LinkedHashMap<>();
            if (segment.startDate() != null) metadata.put("startDate", segment.startDate());
            if (segment.endDate() != null) metadata.put("endDate", segment.endDate());
            add(
                    output,
                    segment.segmentId(),
                    "STRUCTURE",
                    segment.type(),
                    title,
                    segment.accommodationName(),
                    query,
                    metadata);
        }
    }

    private void addPlanning(List<SearchItem> output, UUID tripId, UUID actorUserId, String query) {
        for (TripPlanningView.SearchEntry entry : planning.searchEntries(tripId, actorUserId)) {
            add(
                    output,
                    entry.id(),
                    "PLANNING",
                    entry.type(),
                    title(entry.payload()),
                    subtitle(entry.payload()),
                    query,
                    Map.of("status", entry.status()));
        }
    }

    private static void addWallet(
            List<SearchItem> output,
            List<TripWalletView.Entry> entries,
            String domain,
            String query) {
        for (TripWalletView.Entry entry : entries) {
            add(
                    output,
                    entry.id(),
                    domain,
                    entry.type(),
                    title(entry.payload()),
                    subtitle(entry.payload()),
                    query,
                    Map.of("status", entry.status()));
        }
    }

    private void addExpenses(List<SearchItem> output, UUID tripId, UUID actorUserId, String query) {
        for (TripExpenseView.Entry expense : expenses.searchEntries(tripId, actorUserId)) {
            add(
                    output,
                    expense.expenseId(),
                    "EXPENSE",
                    expense.categoryCode(),
                    expense.title(),
                    expense.note(),
                    query,
                    Map.of("amountMinor", expense.amountMinor(), "currency", expense.currency()));
        }
    }

    private static void add(
            List<SearchItem> output,
            UUID id,
            String domain,
            String type,
            String title,
            String subtitle,
            String query,
            Map<String, Object> highlights) {
        String normalizedTitle = normalize(title);
        String normalizedSubtitle = normalize(subtitle);
        double score =
                normalizedTitle.equals(query)
                        ? 1.0
                        : normalizedTitle.startsWith(query)
                                ? 0.9
                                : normalizedTitle.contains(query)
                                        ? 0.75
                                        : normalizedSubtitle.contains(query) ? 0.55 : 0;
        if (score > 0) {
            output.add(new SearchItem(id, domain, type, title, subtitle, score, highlights));
        }
    }

    private static String title(Map<String, Object> payload) {
        for (String key : List.of("title", "name", "placeName", "merchant")) {
            Object value = payload.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return "이름 없는 항목";
    }

    private static String subtitle(Map<String, Object> payload) {
        for (String key : List.of("description", "note", "url", "address")) {
            Object value = payload.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private static String query(String value) {
        if (value == null
                || value.isBlank()
                || value.strip().length() < 2
                || value.strip().length() > 100) {
            throw EarthTripException.badRequest("INVALID_SEARCH_QUERY", "검색어는 2~100자로 입력해 주세요.");
        }
        return normalize(value);
    }

    private static Set<String> domains(List<String> values) {
        if (values == null || values.isEmpty()) {
            return DOMAINS;
        }
        Set<String> normalized =
                values.stream()
                        .map(value -> value.strip().toUpperCase(Locale.ROOT))
                        .collect(java.util.stream.Collectors.toSet());
        if (!DOMAINS.containsAll(normalized)) {
            throw EarthTripException.badRequest(
                    "INVALID_SEARCH_DOMAIN", "지원하지 않는 검색 영역이 포함되어 있습니다.");
        }
        return normalized;
    }

    private static int limit(Integer value) {
        if (value == null) {
            return 50;
        }
        if (value < 1 || value > 100) {
            throw EarthTripException.badRequest("INVALID_SEARCH_LIMIT", "검색 결과 수는 1~100이어야 합니다.");
        }
        return value;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .strip();
    }
}
