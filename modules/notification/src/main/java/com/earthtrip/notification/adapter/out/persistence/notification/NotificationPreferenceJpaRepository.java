package com.earthtrip.notification.adapter.out.persistence.notification;

import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationPreferenceJpaRepository
        extends JpaRepository<NotificationPreferenceJpaEntity, String> {}
