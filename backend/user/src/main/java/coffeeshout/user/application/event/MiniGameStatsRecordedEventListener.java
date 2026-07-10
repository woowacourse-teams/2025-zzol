package coffeeshout.user.application.event;

import coffeeshout.minigame.event.dto.MiniGameStatsRecordedEvent;
import coffeeshout.user.application.service.UserStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 미니게임 결과 저장(:game) 후 발행되는 이벤트를 구독해 유저 통계를 갱신한다.
 *
 * <p>이전에는 :game의 결과 저장 리스너가 {@link UserStatsService}를 직접 호출해
 * {@code game → user} 컴파일 의존이 있었다. 이벤트 구독으로 방향을 뒤집어
 * :game은 :user를 모르고, :user만 :game-api의 이벤트 계약에 의존한다.
 *
 * <p>발행이 동기(:game의 트랜잭션 안)라 통계 갱신은 기존과 동일하게 결과 저장과
 * 같은 트랜잭션에서 실행된다 — 갱신 실패 시 결과 저장도 함께 롤백된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MiniGameStatsRecordedEventListener {

    private final UserStatsService userStatsService;

    @EventListener
    public void handle(MiniGameStatsRecordedEvent event) {
        event.playerStats().forEach(stat ->
                userStatsService.updateStats(stat.userId(), stat.isWinner()));
    }
}
