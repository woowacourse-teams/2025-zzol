package coffeeshout.racinggame.infra.messaging;

import coffeeshout.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.websocket.docs.WsTopic;
import coffeeshout.websocket.ui.WebSocketResponse;
import coffeeshout.racinggame.domain.event.RaceFinishedEvent;
import coffeeshout.racinggame.domain.event.RaceStateChangedEvent;
import coffeeshout.racinggame.domain.event.RunnersMovedEvent;
import coffeeshout.racinggame.ui.response.RacingGameRunnersStateResponse;
import coffeeshout.racinggame.ui.response.RacingGameStateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RacingGameMessagePublisher {

    public static final String RACING_GAME_PLAYERS_POSITION_DESTINATION_FORMAT = "/topic/room/%s/racing-game";
    public static final String RACING_GAME_STATE_DESTINATION_FORMAT = "/topic/room/%s/racing-game/state";
    private final LoggingSimpMessagingTemplate loggingSimpMessagingTemplate;

    @EventListener
    @WsTopic(
            path = "/room/{joinCode}/racing-game",
            payload = RacingGameRunnersStateResponse.class,
            description = "레이싱 게임 러너 위치 브로드캐스트"
    )
    public void publishRunnersPosition(RunnersMovedEvent runnersMovedEvent) {
        loggingSimpMessagingTemplate.convertAndSend(
                String.format(RACING_GAME_PLAYERS_POSITION_DESTINATION_FORMAT, runnersMovedEvent.joinCode()),
                WebSocketResponse.success(new RacingGameRunnersStateResponse(
                        runnersMovedEvent.racingRange(), runnersMovedEvent.runnerPositions()
                ))
        );
    }

    @EventListener
    @WsTopic(
            path = "/room/{joinCode}/racing-game/state",
            payload = RacingGameStateResponse.class,
            description = "레이싱 게임 상태 변경 브로드캐스트 — state: DESCRIPTION(규칙 설명) | PREPARE(준비) | PLAYING(진행 중) | DONE(종료)"
    )
    public void publishRacingGameStart(RaceStateChangedEvent raceStateChangedEvent) {
        loggingSimpMessagingTemplate.convertAndSend(
                String.format(RACING_GAME_STATE_DESTINATION_FORMAT, raceStateChangedEvent.joinCode()),
                WebSocketResponse.success(new RacingGameStateResponse(raceStateChangedEvent.state()))
        );
    }

    @EventListener
    @WsTopic(
            path = "/room/{joinCode}/racing-game/state",
            payload = RacingGameStateResponse.class,
            description = "레이싱 게임 종료 브로드캐스트 — state: DESCRIPTION(규칙 설명) | PREPARE(준비) | PLAYING(진행 중) | DONE(종료)"
    )
    public void publishRacingGameFinish(RaceFinishedEvent raceFinishedEvent) {
        loggingSimpMessagingTemplate.convertAndSend(
                String.format(RACING_GAME_STATE_DESTINATION_FORMAT, raceFinishedEvent.joinCode()),
                WebSocketResponse.success(new RacingGameStateResponse(raceFinishedEvent.state()))
        );
    }
}
