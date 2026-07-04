package coffeeshout.nunchi.domain.event;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * 눈치게임 press 입력 이벤트. {@code timestamp}는 <b>서버 WS 수신 권위 시각</b>(ADR-0031 Q1·결정 4)으로,
 * 컨트롤러가 스트림 발행 직전 {@link #of}에서 {@code Instant.now()}로 찍는다. 충돌 윈도우 판정은
 * 스트림 도착 순서가 아니라 이 {@code timestamp}로만 한다(N1). {@code BaseEvent}라 자동 직렬화·자동
 * 라우팅된다({@code EventDispatcher}가 concrete 타입으로 {@code Consumer<NunchiCommandEvent>}를 찾음).
 */
public record NunchiCommandEvent(
        String eventId,
        String joinCode,
        String playerName,
        Instant timestamp
) implements BaseEvent {

    public static NunchiCommandEvent of(String joinCode, String playerName) {
        return new NunchiCommandEvent(
                UUID.randomUUID().toString(),
                joinCode,
                playerName,
                Instant.now()
        );
    }
}
