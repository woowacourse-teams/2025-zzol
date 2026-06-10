package coffeeshout.minigame.event;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * 미니게임 시작 커맨드(Redis Stream). 시작은 방 검증(:room) → GameSession 시작(:game)의 두 책임으로
 * 나뉘므로, 검증을 담당하는 {@code :room}이 이 이벤트를 소비할 수 있도록 공유 모듈 {@code :game-api}에 둔다
 * (ADR-0023 결정 4 — 이벤트 분리). 검증 통과 후 {@code :room}이 in-process 동기 {@code GameStartReadyEvent}로
 * {@code :game}에 시작을 위임한다.
 */
public record StartMiniGameCommandEvent(
        String eventId,
        Instant timestamp,
        String joinCode,
        String hostName
) implements BaseEvent {

    public StartMiniGameCommandEvent(String joinCode, String hostName) {
        this(
                UUID.randomUUID().toString(),
                Instant.now(),
                joinCode,
                hostName
        );
    }
}
