package com.earthtrip.identity.adapter.out.persistence.linkedidentity;
import java.util.List;import java.util.Optional;import org.springframework.data.jpa.repository.JpaRepository;
interface LinkedIdentityJpaRepository extends JpaRepository<LinkedIdentityJpaEntity,String>{
 List<LinkedIdentityJpaEntity>findAllByUserIdOrderByCreatedAtAsc(String userId);
 Optional<LinkedIdentityJpaEntity>findByProviderAndProviderSubject(String provider,String subject);
}
