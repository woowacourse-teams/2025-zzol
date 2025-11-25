package coffeeshout.racinggame.infra.messaging;

import coffeeshout.global.ui.WebSocketResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.racinggame.domain.RacingGameState;
import coffeeshout.racinggame.domain.event.RaceFinishedEvent;
import coffeeshout.racinggame.domain.event.RaceStateChangedEvent;
import coffeeshout.racinggame.domain.event.RunnersMovedEvent;
import coffeeshout.racinggame.ui.response.RacingGameRunnersStateResponse;
import coffeeshout.racinggame.ui.response.RacingGameStateResponse;
import generator.annotaions.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RacingGameMessagePublisher {

    private static final String RACING_GAME_PLAYERS_POSITION_DESTINATION_FORMAT = "/topic/room/%s/racing-game";
    private static final String RACING_GAME_STATE_DESTINATION_FORMAT = "/topic/room/%s/racing-game/state";
    private final LoggingSimpMessagingTemplate loggingSimpMessagingTemplate;

    @EventListener
    @MessageResponse(
            path = "/room/{joinCode}/racing-game",
            returnType = RacingGameRunnersStateResponse.class
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
    @MessageResponse(
            path = "/room/{joinCode}/racing-game/state",
            returnType = RacingGameStateResponse.class
    )
    public void publishRacingGameStart(RaceStateChangedEvent raceStateChangedEvent) {
        loggingSimpMessagingTemplate.convertAndSend(
                String.format(RACING_GAME_STATE_DESTINATION_FORMAT, raceStateChangedEvent.joinCode()),
                WebSocketResponse.success(new RacingGameStateResponse(raceStateChangedEvent.state()))
        );
    }

    @EventListener
    @MessageResponse(
            path = "/room/{joinCode}/racing-game/state",
            returnType = WebSocketResponse.class,
            genericType = RacingGameState.class
    )
    public void publishRacingGameFinish(RaceFinishedEvent raceFinishedEvent) {
        loggingSimpMessagingTemplate.convertAndSend(
                String.format(RACING_GAME_STATE_DESTINATION_FORMAT, raceFinishedEvent.joinCode()),
                WebSocketResponse.success(new RacingGameStateResponse(raceFinishedEvent.state()))
        );
    }
}
