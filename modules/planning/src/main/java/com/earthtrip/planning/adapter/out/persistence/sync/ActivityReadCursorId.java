package com.earthtrip.planning.adapter.out.persistence.sync;

import java.io.Serializable;

record ActivityReadCursorId(String tripId, String userId) implements Serializable {}
