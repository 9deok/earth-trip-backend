package com.earthtrip.identity.adapter.out.persistence.deletion;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface AccountDeletionJpaRepository extends JpaRepository<AccountDeletionJpaEntity, String> {

    List<AccountDeletionJpaEntity> findAllByUserIdOrderByRequestedAtDesc(String userId);
}
