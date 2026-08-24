package com.earthtrip.wallet.adapter.out.persistence.template;

import java.util.List;

interface PackingTemplateQuerydslSupport {

    List<PackingTemplateJpaEntity> findVisible(String userId);
}
