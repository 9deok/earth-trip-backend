package com.earthtrip.expense.application.service.category;

import com.earthtrip.expense.application.port.in.ExpenseCategoryUseCase;
import com.earthtrip.expense.application.port.out.ExpenseCategoryStorePort;
import com.earthtrip.expense.application.port.out.ExpenseStorePort;
import com.earthtrip.expense.domain.Expense;
import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ExpenseCategoryService implements ExpenseCategoryUseCase {

    private static final List<SystemCategory> SYSTEM_CATEGORIES = List.of(
        system("AIR", "항공", "#4F46E5", 0),
        system("ACCOMMODATION", "숙소", "#7C3AED", 10),
        system("FOOD", "식비", "#EA580C", 20),
        system("TRANSPORT", "교통", "#0284C7", 30),
        system("ATTRACTION", "관광", "#059669", 40),
        system("SHOPPING", "쇼핑", "#DB2777", 50),
        system("OTHER", "기타", "#64748B", 60)
    );

    private final TripAccess access;
    private final ExpenseCategoryStorePort categories;
    private final ExpenseStorePort expenses;
    private final Clock clock;

    ExpenseCategoryService(
        TripAccess access,
        ExpenseCategoryStorePort categories,
        ExpenseStorePort expenses,
        Clock clock
    ) {
        this.access = access;
        this.categories = categories;
        this.expenses = expenses;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResult> list(UUID tripId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        Instant now = clock.instant();
        List<CategoryResult> results = new java.util.ArrayList<>(
            SYSTEM_CATEGORIES.stream().map(category -> result(tripId, category, now)).toList()
        );
        results.addAll(categories.findAll(tripId).stream().map(ExpenseCategoryService::result).toList());
        return results.stream()
            .sorted(Comparator.comparingInt(CategoryResult::sortOrder)
                .thenComparing(CategoryResult::name))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResult get(UUID tripId, UUID categoryId, UUID actorUserId) {
        access.requireViewer(tripId, actorUserId);
        SystemCategory system = SYSTEM_CATEGORIES.stream()
            .filter(category -> category.id().equals(categoryId))
            .findFirst()
            .orElse(null);
        if (system != null) {
            return result(tripId, system, clock.instant());
        }
        return result(loadCustom(tripId, categoryId));
    }

    @Override
    public CategoryResult create(
        UUID tripId,
        UUID actorUserId,
        UUID requestId,
        String name,
        String color,
        Integer sortOrder
    ) {
        access.requireEditor(tripId, actorUserId);
        if (requestId == null) {
            throw EarthTripException.badRequest("REQUEST_ID_REQUIRED", "요청 ID가 필요합니다.");
        }
        String safeName = name(name);
        String safeColor = color(color);
        ExpenseCategoryStorePort.CategoryRecord existing = categories.findById(requestId).orElse(null);
        if (existing != null) {
            if (!existing.tripId().equals(tripId)
                || !existing.name().equals(safeName)
                || !existing.color().equals(safeColor)
                || sortOrder != null && existing.sortOrder() != order(sortOrder)) {
                throw idempotencyConflict();
            }
            return result(existing);
        }
        int safeOrder = sortOrder == null ? nextSortOrder(tripId) : order(sortOrder);
        Instant now = clock.instant();
        String code = "CUSTOM_" + requestId.toString().replace("-", "").toUpperCase(Locale.ROOT);
        return result(categories.save(new ExpenseCategoryStorePort.CategoryRecord(
            requestId, tripId, code, safeName, safeColor, safeOrder,
            actorUserId, actorUserId, now, now, null, 0
        )));
    }

    @Override
    public CategoryResult update(
        UUID tripId,
        UUID categoryId,
        UUID actorUserId,
        String name,
        String color,
        Integer sortOrder,
        long baseVersion
    ) {
        access.requireEditor(tripId, actorUserId);
        rejectSystemMutation(categoryId);
        ExpenseCategoryStorePort.CategoryRecord current = loadCustom(tripId, categoryId);
        requireVersion(current, baseVersion);
        Instant now = clock.instant();
        return result(categories.save(new ExpenseCategoryStorePort.CategoryRecord(
            current.id(), current.tripId(), current.code(),
            name == null ? current.name() : name(name),
            color == null ? current.color() : color(color),
            sortOrder == null ? current.sortOrder() : order(sortOrder),
            current.createdBy(), actorUserId, current.createdAt(), now, null, current.version()
        )));
    }

    @Override
    public void delete(
        UUID tripId,
        UUID categoryId,
        UUID actorUserId,
        String replacementCode,
        long baseVersion
    ) {
        access.requireEditor(tripId, actorUserId);
        rejectSystemMutation(categoryId);
        ExpenseCategoryStorePort.CategoryRecord current = loadCustom(tripId, categoryId);
        requireVersion(current, baseVersion);
        List<Expense> affected = expenses.findAll(tripId).stream()
            .filter(expense -> expense.categoryCode().equals(current.code()))
            .toList();
        String replacement = replacementCode == null ? null : requireCategoryCode(tripId, replacementCode);
        if (!affected.isEmpty() && replacement == null) {
            throw EarthTripException.conflict(
                "EXPENSE_CATEGORY_IN_USE",
                "이 분류를 사용하는 지출이 있습니다. 대체 분류를 지정해 주세요."
            );
        }
        if (current.code().equals(replacement)) {
            throw EarthTripException.badRequest(
                "SAME_REPLACEMENT_CATEGORY",
                "삭제할 분류와 대체 분류는 달라야 합니다."
            );
        }
        Instant now = clock.instant();
        for (Expense expense : affected) {
            expense.update(
                expense.title(), replacement, expense.amountMinor(), expense.currency(),
                expense.occurredAt(), expense.payers(), expense.shares(), expense.visibility(),
                expense.status(), expense.note(), actorUserId, now
            );
            expenses.save(expense);
        }
        categories.save(new ExpenseCategoryStorePort.CategoryRecord(
            current.id(), current.tripId(), current.code(), current.name(), current.color(),
            current.sortOrder(), current.createdBy(), actorUserId, current.createdAt(), now,
            now, current.version()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public String requireCategoryCode(UUID tripId, String code) {
        String normalized = code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
        if (SYSTEM_CATEGORIES.stream().anyMatch(category -> category.code().equals(normalized))) {
            return normalized;
        }
        return categories.findByCode(tripId, normalized)
            .map(ExpenseCategoryStorePort.CategoryRecord::code)
            .orElseThrow(() -> EarthTripException.badRequest(
                "EXPENSE_CATEGORY_NOT_FOUND",
                "사용할 수 있는 지출 분류가 아닙니다."
            ));
    }

    private ExpenseCategoryStorePort.CategoryRecord loadCustom(UUID tripId, UUID categoryId) {
        return categories.findById(categoryId)
            .filter(category -> category.tripId().equals(tripId))
            .orElseThrow(() -> EarthTripException.notFound(
                "EXPENSE_CATEGORY_NOT_FOUND",
                "지출 분류를 찾을 수 없습니다."
            ));
    }

    private int nextSortOrder(UUID tripId) {
        return categories.findAll(tripId).stream()
            .mapToInt(ExpenseCategoryStorePort.CategoryRecord::sortOrder)
            .max()
            .orElse(90) + 10;
    }

    private static void rejectSystemMutation(UUID categoryId) {
        if (SYSTEM_CATEGORIES.stream().anyMatch(category -> category.id().equals(categoryId))) {
            throw EarthTripException.conflict(
                "SYSTEM_CATEGORY_IMMUTABLE",
                "기본 지출 분류는 변경하거나 삭제할 수 없습니다."
            );
        }
    }

    private static String name(String value) {
        if (value == null || value.isBlank() || value.strip().length() > 100) {
            throw EarthTripException.badRequest(
                "INVALID_EXPENSE_CATEGORY_NAME",
                "분류 이름은 1자 이상 100자 이하여야 합니다."
            );
        }
        return value.strip();
    }

    private static String color(String value) {
        String normalized = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
        if (!normalized.matches("#[0-9A-F]{6}")) {
            throw EarthTripException.badRequest(
                "INVALID_EXPENSE_CATEGORY_COLOR",
                "분류 색상은 #RRGGBB 형식이어야 합니다."
            );
        }
        return normalized;
    }

    private static int order(int value) {
        if (value < 0) {
            throw EarthTripException.badRequest(
                "INVALID_EXPENSE_CATEGORY_ORDER",
                "분류 정렬 순서는 0 이상이어야 합니다."
            );
        }
        return value;
    }

    private static void requireVersion(
        ExpenseCategoryStorePort.CategoryRecord category,
        long baseVersion
    ) {
        if (category.version() != baseVersion) {
            throw new EarthTripException(
                "VERSION_CONFLICT",
                409,
                "다른 지출 분류 변경이 먼저 저장되었습니다.",
                Map.of("serverVersion", category.version())
            );
        }
    }

    private static CategoryResult result(ExpenseCategoryStorePort.CategoryRecord category) {
        return new CategoryResult(
            category.id(), category.tripId(), category.code(), category.name(), category.color(),
            category.sortOrder(), false, category.version(), category.updatedBy(),
            category.updatedAt()
        );
    }

    private static CategoryResult result(UUID tripId, SystemCategory category, Instant now) {
        return new CategoryResult(
            category.id(), tripId, category.code(), category.name(), category.color(),
            category.sortOrder(), true, 0, null, now
        );
    }

    private static SystemCategory system(String code, String name, String color, int order) {
        UUID id = UUID.nameUUIDFromBytes(
            ("earthtrip:expense-category:" + code).getBytes(StandardCharsets.UTF_8)
        );
        return new SystemCategory(id, code, name, color, order);
    }

    private static EarthTripException idempotencyConflict() {
        return EarthTripException.conflict(
            "IDEMPOTENCY_KEY_REUSED",
            "이미 다른 지출 분류에 사용된 요청 ID입니다."
        );
    }

    private record SystemCategory(
        UUID id,
        String code,
        String name,
        String color,
        int sortOrder
    ) { }
}
