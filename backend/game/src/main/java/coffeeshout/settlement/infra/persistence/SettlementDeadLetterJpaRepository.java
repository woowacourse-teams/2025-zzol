package coffeeshout.settlement.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementDeadLetterJpaRepository extends JpaRepository<SettlementDeadLetterEntity, Long> {
}
