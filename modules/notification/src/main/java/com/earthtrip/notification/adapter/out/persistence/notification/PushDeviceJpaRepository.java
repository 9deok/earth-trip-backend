package com.earthtrip.notification.adapter.out.persistence.notification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface PushDeviceJpaRepository extends JpaRepository<PushDeviceJpaEntity, String> {

    List<PushDeviceJpaEntity> findAllByUserIdAndActiveTrue(String userId);

    Optional<PushDeviceJpaEntity> findByTokenHash(String tokenHash);
}
