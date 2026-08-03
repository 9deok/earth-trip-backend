package com.earthtrip.wallet.adapter.out.persistence.template;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PackingTemplateJpaRepository extends JpaRepository<PackingTemplateJpaEntity, String> {

    @Query("""
        select template from PackingTemplateJpaEntity template
        where template.deletedAt is null
          and (template.userId = :userId or template.visibility = 'PUBLIC')
        order by template.updatedAt desc
        """)
    List<PackingTemplateJpaEntity> findVisible(@Param("userId") String userId);
}
