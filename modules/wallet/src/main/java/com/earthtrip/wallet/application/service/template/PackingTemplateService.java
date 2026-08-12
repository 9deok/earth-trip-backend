package com.earthtrip.wallet.application.service.template;

import com.earthtrip.sharedkernel.error.EarthTripException;
import com.earthtrip.trip.api.TripAccess;
import com.earthtrip.wallet.application.port.in.PackingTemplateUseCase;
import com.earthtrip.wallet.application.port.in.WalletRecordUseCase;
import com.earthtrip.wallet.application.port.out.PackingTemplateStorePort;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class PackingTemplateService implements PackingTemplateUseCase {

    private static final Set<String> VISIBILITIES = Set.of("PERSONAL", "PUBLIC");

    private final TripAccess access;
    private final WalletRecordUseCase wallet;
    private final PackingTemplateStorePort store;
    private final Clock clock;

    PackingTemplateService(
            TripAccess access,
            WalletRecordUseCase wallet,
            PackingTemplateStorePort store,
            Clock clock) {
        this.access = access;
        this.wallet = wallet;
        this.store = store;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResult> list(UUID actorUserId) {
        return store.findVisible(actorUserId).stream()
                .map(record -> result(record, actorUserId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateResult get(UUID templateId, UUID actorUserId) {
        return result(requireVisible(templateId, actorUserId), actorUserId);
    }

    @Override
    public TemplateResult create(UUID actorUserId, TemplateCommand command) {
        if (command.requestId() == null) {
            throw EarthTripException.badRequest("REQUEST_ID_REQUIRED", "requestId가 필요합니다.");
        }
        PackingTemplateStorePort.TemplateRecord existing =
                store.findById(command.requestId()).orElse(null);
        if (existing != null) {
            if (!existing.userId().equals(actorUserId)) {
                throw EarthTripException.conflict(
                        "IDEMPOTENCY_KEY_REUSED", "이미 다른 짐 템플릿에 사용된 요청 ID입니다.");
            }
            return result(existing, actorUserId);
        }
        Instant now = clock.instant();
        return result(
                store.save(
                        new PackingTemplateStorePort.TemplateRecord(
                                command.requestId(),
                                actorUserId,
                                name(command.name()),
                                visibility(command.visibility()),
                                items(command.items()),
                                now,
                                now,
                                null,
                                0)),
                actorUserId);
    }

    @Override
    public TemplateResult update(UUID templateId, UUID actorUserId, TemplateCommand command) {
        PackingTemplateStorePort.TemplateRecord current = requireOwned(templateId, actorUserId);
        version(current.version(), command.baseVersion());
        return result(
                store.save(
                        new PackingTemplateStorePort.TemplateRecord(
                                current.id(),
                                current.userId(),
                                command.name() == null ? current.name() : name(command.name()),
                                command.visibility() == null
                                        ? current.visibility()
                                        : visibility(command.visibility()),
                                command.items() == null ? current.items() : items(command.items()),
                                current.createdAt(),
                                clock.instant(),
                                null,
                                current.version())),
                actorUserId);
    }

    @Override
    public void delete(UUID templateId, UUID actorUserId, long baseVersion) {
        PackingTemplateStorePort.TemplateRecord current = requireOwned(templateId, actorUserId);
        version(current.version(), baseVersion);
        store.save(
                new PackingTemplateStorePort.TemplateRecord(
                        current.id(),
                        current.userId(),
                        current.name(),
                        current.visibility(),
                        current.items(),
                        current.createdAt(),
                        clock.instant(),
                        clock.instant(),
                        current.version()));
    }

    @Override
    public ApplicationResult apply(UUID tripId, UUID actorUserId, ApplicationCommand command) {
        access.requireEditor(tripId, actorUserId);
        if (command.requestId() == null || command.templateId() == null) {
            throw EarthTripException.badRequest(
                    "INVALID_TEMPLATE_APPLICATION", "requestId와 templateId가 필요합니다.");
        }
        PackingTemplateStorePort.ApplicationRecord existing =
                store.findApplication(command.requestId()).orElse(null);
        if (existing != null) {
            if (!existing.tripId().equals(tripId)
                    || !existing.templateId().equals(command.templateId())) {
                throw EarthTripException.conflict(
                        "IDEMPOTENCY_KEY_REUSED", "이미 다른 템플릿 적용에 사용된 요청 ID입니다.");
            }
            return application(existing);
        }
        PackingTemplateStorePort.TemplateRecord template =
                requireVisible(command.templateId(), actorUserId);
        String recordVisibility = walletVisibility(command.visibility());
        Set<String> existingNames =
                wallet.list(tripId, actorUserId, "PACKING_ITEM", null).stream()
                        .map(WalletRecordUseCase.RecordResult::payload)
                        .map(payload -> payload.get("name"))
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .map(PackingTemplateService::normalizedItemName)
                        .collect(java.util.stream.Collectors.toSet());
        List<UUID> itemIds = new ArrayList<>();
        for (int index = 0; index < template.items().size(); index++) {
            TemplateItem item = template.items().get(index);
            if (!existingNames.add(normalizedItemName(item.name()))) {
                continue;
            }
            UUID itemId = itemId(command.requestId(), index);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("name", item.name());
            payload.put("category", item.category());
            payload.put("quantity", item.quantity());
            if (item.note() != null) {
                payload.put("note", item.note());
            }
            payload.put("templateId", template.id().toString());
            wallet.create(
                    tripId,
                    actorUserId,
                    "PACKING_ITEM",
                    true,
                    new WalletRecordUseCase.Command(
                            itemId, null, payload, "OPEN", recordVisibility, index, 0));
            itemIds.add(itemId);
        }
        PackingTemplateStorePort.ApplicationRecord saved =
                store.saveApplication(
                        new PackingTemplateStorePort.ApplicationRecord(
                                command.requestId(),
                                tripId,
                                template.id(),
                                actorUserId,
                                clock.instant(),
                                List.copyOf(itemIds)));
        return application(saved);
    }

    private PackingTemplateStorePort.TemplateRecord requireVisible(
            UUID templateId, UUID actorUserId) {
        PackingTemplateStorePort.TemplateRecord template =
                store.findById(templateId).orElseThrow(PackingTemplateService::notFound);
        if (!template.userId().equals(actorUserId) && !template.visibility().equals("PUBLIC")) {
            throw notFound();
        }
        return template;
    }

    private PackingTemplateStorePort.TemplateRecord requireOwned(
            UUID templateId, UUID actorUserId) {
        PackingTemplateStorePort.TemplateRecord template =
                store.findById(templateId)
                        .filter(item -> item.userId().equals(actorUserId))
                        .orElseThrow(PackingTemplateService::notFound);
        return template;
    }

    private static List<TemplateItem> items(List<TemplateItem> values) {
        if (values == null || values.size() > 200) {
            throw EarthTripException.badRequest(
                    "INVALID_TEMPLATE_ITEMS", "짐 템플릿에는 최대 200개의 항목을 저장할 수 있습니다.");
        }
        return values.stream().map(PackingTemplateService::item).toList();
    }

    private static String normalizedItemName(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private static TemplateItem item(TemplateItem item) {
        if (item == null) {
            throw EarthTripException.badRequest("INVALID_TEMPLATE_ITEM", "비어 있는 짐 항목이 포함되어 있습니다.");
        }
        String itemName = text(item.name(), 120, "짐 이름");
        String category =
                item.category() == null || item.category().isBlank()
                        ? "OTHER"
                        : text(item.category(), 60, "카테고리").toUpperCase(Locale.ROOT);
        if (item.quantity() < 1 || item.quantity() > 999) {
            throw EarthTripException.badRequest("INVALID_PACKING_QUANTITY", "짐 수량은 1~999여야 합니다.");
        }
        String note =
                item.note() == null || item.note().isBlank() ? null : text(item.note(), 500, "메모");
        return new TemplateItem(itemName, category, item.quantity(), note);
    }

    private static String name(String value) {
        return text(value, 120, "템플릿 이름");
    }

    private static String visibility(String value) {
        String normalized = value == null ? "PERSONAL" : value.strip().toUpperCase(Locale.ROOT);
        if (!VISIBILITIES.contains(normalized)) {
            throw EarthTripException.badRequest(
                    "INVALID_TEMPLATE_VISIBILITY", "PERSONAL 또는 PUBLIC만 선택할 수 있습니다.");
        }
        return normalized;
    }

    private static String walletVisibility(String value) {
        String normalized = value == null ? "TRIP" : value.strip().toUpperCase(Locale.ROOT);
        if (!Set.of("PRIVATE", "PARTICIPANTS", "TRIP").contains(normalized)) {
            throw EarthTripException.badRequest(
                    "INVALID_PACKING_VISIBILITY", "지원하지 않는 짐 공개 범위입니다.");
        }
        return normalized;
    }

    private static String text(String value, int max, String label) {
        if (value == null || value.isBlank() || value.strip().length() > max) {
            throw EarthTripException.badRequest("INVALID_TEMPLATE_FIELD", label + "을(를) 확인해 주세요.");
        }
        return value.strip();
    }

    private static void version(long serverVersion, long baseVersion) {
        if (serverVersion != baseVersion) {
            throw new EarthTripException(
                    "VERSION_CONFLICT",
                    409,
                    "다른 템플릿 변경이 먼저 저장되었습니다.",
                    Map.of("serverVersion", serverVersion));
        }
    }

    private static UUID itemId(UUID applicationId, int index) {
        return UUID.nameUUIDFromBytes(
                ("earthtrip:packing-template:" + applicationId + ":" + index)
                        .getBytes(StandardCharsets.UTF_8));
    }

    private static TemplateResult result(
            PackingTemplateStorePort.TemplateRecord record, UUID actorUserId) {
        return new TemplateResult(
                record.id(),
                record.userId(),
                record.name(),
                record.visibility(),
                record.items(),
                record.userId().equals(actorUserId),
                record.version(),
                record.createdAt(),
                record.updatedAt());
    }

    private static ApplicationResult application(
            PackingTemplateStorePort.ApplicationRecord record) {
        return new ApplicationResult(
                record.id(), record.templateId(), record.itemIds(), record.appliedAt());
    }

    private static EarthTripException notFound() {
        return EarthTripException.notFound("PACKING_TEMPLATE_NOT_FOUND", "짐 템플릿을 찾을 수 없습니다.");
    }
}
