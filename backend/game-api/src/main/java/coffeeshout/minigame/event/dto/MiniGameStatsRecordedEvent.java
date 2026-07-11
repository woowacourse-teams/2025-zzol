package coffeeshout.minigame.event.dto;

import java.util.List;

/**
 * 미니게임 결과가 저장된 뒤, 회원 플레이어의 승패를 전달하는 이벤트.
 *
 * <p>:game이 결과 저장 직후 발행하고 :user가 구독해 유저 통계를 갱신한다.
 * 이를 통해 :game은 :user를 직접 참조하지 않는다(의존 역전, ADR-0025 계열).
 * 게스트(userId 없음)는 통계 대상이 아니므로 목록에 포함하지 않는다.
 */
public record MiniGameStatsRecordedEvent(List<PlayerStat> playerStats) {

    public record PlayerStat(Long userId, boolean isWinner) {
    }
}
