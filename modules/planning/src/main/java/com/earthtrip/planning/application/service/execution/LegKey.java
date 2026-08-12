package com.earthtrip.planning.application.service.execution;

import java.util.UUID;

record LegKey(UUID fromItemId, UUID toItemId) {}
