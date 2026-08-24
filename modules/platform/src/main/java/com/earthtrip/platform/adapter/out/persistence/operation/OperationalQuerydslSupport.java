package com.earthtrip.platform.adapter.out.persistence.operation;

import java.util.List;

interface OperationalQuerydslSupport {

    List<OperationalJobJpaEntity> searchJobs(String status, String jobType, int limit);

    List<DeadLetterJpaEntity> searchDeadLetters(String status, int limit);

    List<AdminAuditJpaEntity> searchAudits(
            String action, String targetType, String targetId, int limit);
}
