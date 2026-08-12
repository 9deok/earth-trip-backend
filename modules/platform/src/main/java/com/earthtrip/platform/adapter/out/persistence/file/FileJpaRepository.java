package com.earthtrip.platform.adapter.out.persistence.file;

import org.springframework.data.jpa.repository.JpaRepository;

interface FileJpaRepository extends JpaRepository<FileJpaEntity, String> {}
