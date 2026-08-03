package com.earthtrip.platform.adapter.out.persistence.file;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface FileLinkJpaRepository extends JpaRepository<FileLinkJpaEntity, String> {

    List<FileLinkJpaEntity> findAllByFileIdOrderByLinkedAtAsc(String fileId);

    List<FileLinkJpaEntity> findAllByTripIdOrderByLinkedAtAsc(String tripId);

    List<FileLinkJpaEntity> findAllByTripIdAndResourceTypeAndResourceIdOrderByLinkedAtAsc(
        String tripId,
        String resourceType,
        String resourceId
    );
}
