package com.earthtrip.wallet.adapter.out.persistence.template;

import org.springframework.data.jpa.repository.JpaRepository;

interface PreparationSuggestionDismissalJpaRepository
        extends JpaRepository<
                PreparationSuggestionDismissalJpaEntity, PreparationSuggestionDismissalId> {}
