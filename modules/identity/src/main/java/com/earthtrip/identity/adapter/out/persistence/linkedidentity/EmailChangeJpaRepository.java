package com.earthtrip.identity.adapter.out.persistence.linkedidentity;
import java.util.Optional;import org.springframework.data.jpa.repository.JpaRepository;
interface EmailChangeJpaRepository extends JpaRepository<EmailChangeJpaEntity,String>{
 Optional<EmailChangeJpaEntity>findByTokenHash(String tokenHash);
 Optional<EmailChangeJpaEntity>findFirstByUserIdAndStatusOrderByCreatedAtDesc(String userId,String status);
}
