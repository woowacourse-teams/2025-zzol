package coffeeshout.settlement.application;

import coffeeshout.settlement.domain.SeasonKey;
import coffeeshout.settlement.domain.SeasonPointPolicy;
import coffeeshout.settlement.domain.SeasonTier;
import coffeeshout.settlement.event.SettlementResultEvent;
import coffeeshout.settlement.event.SettlementResultEvent.PlayerResult;
import coffeeshout.settlement.infra.persistence.SeasonScoreEntity;
import coffeeshout.settlement.infra.persistence.SeasonScoreJpaRepository;
import coffeeshout.settlement.infra.persistence.SeasonSettlementEntity;
import coffeeshout.settlement.infra.persistence.SeasonSettlementJpaRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시즌 정산의 유일한 쓰기 경로. 컨슈머 그룹이 전달한 게임 결과를 포인트 원장과 시즌 누적
 * 성적에 반영한다(#1610).
 * <p>
 * at-least-once 전달을 전제로 설계했다. 재전달·부분 처리 후 재시도의 모든 경로에서:
 * <ul>
 *   <li>이미 정산된 결과는 원장 조회로 건너뛴다 — 크래시로 일부만 정산된 이벤트가
 *       재전달되면 남은 행만 이어서 정산된다.</li>
 *   <li>조회를 빠져나간 동시 경합은 원장의 유니크 제약이 막는다. 제약 위반은 여기서 삼키지
 *       않고 전파한다 — 컨슈머가 ACK하지 않고 재전달받으면 그때는 조회가 걸러낸다.</li>
 * </ul>
 * 시즌 키는 처리 시각이 아니라 이벤트 시각에서 파생한다. 시즌 경계(월말)를 넘겨 재처리해도
 * 같은 시즌으로 정산되어야 멱등이 성립하기 때문이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SeasonSettlementJpaRepository settlementRepository;
    private final SeasonScoreJpaRepository scoreRepository;

    @Transactional
    public List<SettledScore> settle(SettlementResultEvent event) {
        final SeasonKey seasonKey = SeasonKey.from(event.timestamp());
        final List<SettledScore> settled = new ArrayList<>();

        for (PlayerResult result : event.results()) {
            if (alreadySettled(event, result)) {
                log.info("이미 정산된 결과 — 건너뜀: eventId={}, userId={}", event.eventId(), result.userId());
                continue;
            }
            settled.add(settleOne(event, result, seasonKey));
        }
        return settled;
    }

    private boolean alreadySettled(SettlementResultEvent event, PlayerResult result) {
        return settlementRepository.existsByRoomSessionIdAndMiniGameTypeAndUserId(
                event.roomSessionId(), event.miniGameType(), result.userId());
    }

    private SettledScore settleOne(SettlementResultEvent event, PlayerResult result, SeasonKey seasonKey) {
        final int points = SeasonPointPolicy.pointsFor(result.rank());

        settlementRepository.save(new SeasonSettlementEntity(
                event.roomSessionId(),
                event.miniGameType(),
                result.userId(),
                result.rank(),
                result.score(),
                points,
                seasonKey.value()
        ));

        // 가산은 DB 원자 UPDATE — 같은 회원의 다른 게임 결과가 동시에 정산되어도 유실이 없다.
        // 0행이면 이 시즌 첫 정산이라 새 행을 만든다(동시 INSERT 경합은 유니크 제약 → 재전달로 수렴).
        final int updated = scoreRepository.addPoints(seasonKey.value(), result.userId(), points);
        if (updated == 0) {
            scoreRepository.save(new SeasonScoreEntity(seasonKey.value(), result.userId(), points));
        }

        final SeasonScoreEntity score = scoreRepository.findBySeasonKeyAndUserId(seasonKey.value(), result.userId())
                .orElseThrow(() -> new IllegalStateException(
                        "정산 직후 시즌 성적이 존재해야 합니다: userId=" + result.userId()));

        // 티어는 가산 결과의 파생값이라 가산 후 재판정한다.
        final SeasonTier tier = SeasonTier.fromPoints(score.getTotalPoints());
        if (score.getTier() != tier) {
            score.updateTier(tier);
        }

        return new SettledScore(result.userId(), seasonKey.value(), score.getTotalPoints(), tier);
    }

    /**
     * 정산이 실제로 반영된 회원의 최신 누적 성적. 리더보드(ZSET) 갱신·알림의 입력이 된다 —
     * 절대값(totalPoints)을 넘겨 ZSET 갱신이 재실행에도 안전(멱등)하도록 한다.
     */
    public record SettledScore(long userId, String seasonKey, long totalPoints, SeasonTier tier) {
    }
}
