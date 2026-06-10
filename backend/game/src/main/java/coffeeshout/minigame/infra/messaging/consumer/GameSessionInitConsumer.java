package coffeeshout.minigame.infra.messaging.consumer;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.GameRoomCreatedEvent;
import coffeeshout.minigame.application.GameSessionService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 방 생성 시 GameSession을 사전 초기화한다(ADR-0025 결정 6 — 생명주기 자동 관리).
 *
 * <p>호스트 검증이 이름 기준이므로 {@code hostName}만으로 host를 구성한다(회원 여부 무관 — ADR-0025 결정 2).
 * {@code initSession}은 멱등이라 {@code updateGames}의 지연 생성과 충돌하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class GameSessionInitConsumer implements Consumer<GameRoomCreatedEvent> {

    private final GameSessionService gameSessionService;

    @Override
    public void accept(GameRoomCreatedEvent event) {
        gameSessionService.initSession(new JoinCode(event.joinCode()), Gamer.guest(event.hostName()));
    }
}
