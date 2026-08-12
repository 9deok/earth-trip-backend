package com.earthtrip.platform.adapter.out.persistence.file;

import org.springframework.data.jpa.repository.JpaRepository;

interface UploadSessionJpaRepository extends JpaRepository<UploadSessionJpaEntity, String> {}
