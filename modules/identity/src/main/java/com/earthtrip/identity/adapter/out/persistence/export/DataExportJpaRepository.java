package com.earthtrip.identity.adapter.out.persistence.export;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface DataExportJpaRepository extends JpaRepository<DataExportJpaEntity, String> {

    List<DataExportJpaEntity> findAllByUserIdOrderByCreatedAtDesc(String userId);
}
