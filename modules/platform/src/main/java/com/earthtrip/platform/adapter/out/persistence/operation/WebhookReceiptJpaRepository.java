package com.earthtrip.platform.adapter.out.persistence.operation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface WebhookReceiptJpaRepository
    extends JpaRepository<WebhookReceiptJpaEntity, String> {

    Optional<WebhookReceiptJpaEntity> findByProviderAndSourceEventId(
        String provider,
        String sourceEventId
    );
}
