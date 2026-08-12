package com.earthtrip.planning.application.service.execution;

import java.util.Map;
import java.util.UUID;

record Node(
        UUID id,
        Double lat,
        Double lon,
        Integer start,
        Integer end,
        Integer openStart,
        Integer openEnd,
        boolean fixed,
        Map<String, Object> payload) {}
