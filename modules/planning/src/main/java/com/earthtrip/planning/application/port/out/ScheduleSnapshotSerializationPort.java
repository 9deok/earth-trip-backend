package com.earthtrip.planning.application.port.out;

import java.util.List;
import java.util.UUID;

public interface ScheduleSnapshotSerializationPort {
    String serialize(List<SnapshotItem> items);

    List<SnapshotItem> deserialize(String value);

    record SnapshotItem(UUID itemId, int sortOrder, long version) {}
}
