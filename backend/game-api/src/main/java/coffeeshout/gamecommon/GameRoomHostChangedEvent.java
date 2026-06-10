package coffeeshout.gamecommon;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * 방의 호스트가 승계됐음을 알리는 공유 생명주기 이벤트(ADR-0025 결정 6). 기존 호스트가 방을 떠나
 * {@code Room.promoteNewHost()}로 새 호스트가 지정되면 {@code :room}이 발행하고, {@code :game}이 소비해
 * GameSession의 호스트를 갱신한다(세션 호스트가 방 생성 시점 값에 고정돼 새 호스트의 세션 조작이 거부되던 문제 해소).
 *
 * <p>GameSession은 인스턴스 로컬 저장소에 있으므로 갱신도 생성·정리와 동일한 Stream 경로를 타야
 * 세션을 소유한 모든 인스턴스에 도달한다(in-process 리스너 금지). 호스트 식별은 이름 기준이므로
 * {@code newHostName}만 싣는다(결정 2).
 */
public record GameRoomHostChangedEvent(
        String eventId,
        Instant timestamp,
        String joinCode,
        String newHostName
) implements BaseEvent {

    public GameRoomHostChangedEvent(String joinCode, String newHostName) {
        this(
                UUID.randomUUID().toString(),
                Instant.now(),
                joinCode,
                newHostName
        );
    }
}
