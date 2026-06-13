package coffeeshout.gamecommon;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * 방 생명주기를 통지하는 공유 이벤트 패밀리(ADR-0025 결정 6). 모두 이미 일어난 사실을 통지한다
 * — {@code :room}이 상태 변경을 완료한 뒤 발행하고, {@code :room}·{@code :game}이 소비한다. 두 모듈이
 * 모두 의존하는 {@code :game-api}에 두어 어느 한쪽 모듈 내부에 종속되지 않게 한다.
 *
 * <p>닫힌 패밀리를 sealed로 표현해 통지 종류(생성·삭제·호스트 승계)를 타입으로 묶는다. 각 이벤트는
 * 호스트 검증이 이름 기준이므로(결정 2) {@code userId} 없이 이름만 싣는다.
 */
public sealed interface RoomLifecycleEvent extends BaseEvent {

    /**
     * 방이 생성됐음을 통지한다. {@code :room}(방 등록)과 {@code :game}(GameSession 사전 생성)이 함께 소비한다.
     */
    record Created(
            String eventId,
            Instant timestamp,
            String hostName,
            String joinCode
    ) implements RoomLifecycleEvent {

        public Created(String hostName, String joinCode) {
            this(UUID.randomUUID().toString(), Instant.now(), hostName, joinCode);
        }
    }

    /**
     * 방이 삭제됐음을 통지한다. {@code :game}이 소비해 GameSession을 정리한다(인스턴스 로컬 세션이라
     * 생성과 동일한 Stream 경로로 세션 소유 인스턴스에 도달해야 한다).
     */
    record Removed(
            String eventId,
            Instant timestamp,
            String joinCode
    ) implements RoomLifecycleEvent {

        public Removed(String joinCode) {
            this(UUID.randomUUID().toString(), Instant.now(), joinCode);
        }
    }

    /**
     * 방의 호스트가 승계됐음을 통지한다. 기존 호스트가 떠나 {@code Room.promoteNewHost()}로 새 호스트가
     * 지정되면 발행되고, {@code :game}이 소비해 GameSession 호스트를 갱신한다(세션 호스트가 생성 시점
     * 값에 고정돼 새 호스트의 조작이 거부되던 문제 해소). 식별은 이름 기준이라 {@code newHostName}만 싣는다.
     */
    record HostChanged(
            String eventId,
            Instant timestamp,
            String joinCode,
            String newHostName
    ) implements RoomLifecycleEvent {

        public HostChanged(String joinCode, String newHostName) {
            this(UUID.randomUUID().toString(), Instant.now(), joinCode, newHostName);
        }
    }
}
