package coffeeshout.settlement.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonSettlementJpaRepository extends JpaRepository<SeasonSettlementEntity, Long> {

    boolean existsByRoomSessionIdAndMiniGameTypeAndUserId(Long roomSessionId, String miniGameType, Long userId);
}
