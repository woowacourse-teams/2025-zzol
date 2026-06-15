package coffeeshout.minigame.infra.messaging.consumer;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.RoomLifecycleEvent;
import coffeeshout.minigame.application.GameSessionService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 호스트 승계 시 GameSession의 호스트를 갱신한다(ADR-0025 결정 6 — 생명주기 자동 관리).
 *
 * <p>호스트 검증이 이름 기준이므로 {@code newHostName}만으로 host를 구성한다({@code Gamer.guest}).
 * GameSession은 인스턴스 로컬 저장소에 있으므로 갱신도 생성·정리와 동일한 Stream 경로를 타야
 * 세션을 소유한 인스턴스에 도달한다. {@code updateHost}는 세션이 없어도 안전하다(멱등·비throw).
 */
@Component
@RequiredArgsConstructor
public class GameSessionHostChangeConsumer implements Consumer<RoomLifecycleEvent.HostChanged> {

    private final GameSessionService gameSessionService;

    @Override
    public void accept(RoomLifecycleEvent.HostChanged event) {
        gameSessionService.updateHost(new JoinCode(event.joinCode()), Gamer.guest(event.newHostName()));
    }
}
