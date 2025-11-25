package coffeeshout.minigame.ui;

import coffeeshout.minigame.ui.command.MiniGameCommand;
import coffeeshout.minigame.ui.command.MiniGameCommandDispatcher;
import coffeeshout.minigame.ui.request.MiniGameMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import generator.annotaions.MessageResponse;
import generator.annotaions.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MiniGameWebSocketController {

    private final MiniGameCommandDispatcher miniGameCommandDispatcher;
    private final ObjectMapper objectMapper;

    @MessageMapping("/room/{joinCode}/minigame/command")
    @Operation(
            summary = "미니게임을 시작, 조작하는데 필요한 명령어 실행 요청",
            description = """
                    미니게임 시작 및 미니게임에서 발생하는 명령(카드 선택)을 실행하는 웹소켓 요청입니다.
                    사용하고자 하는 명령어 종류에 맞게 CommandType을 선택하여 요청을 보내면 된다.

                    StartMiniGameCommand: /topic/room/{joinCode}/round 로 MiniGameStartMessage 전송
                    SelectCardCommand: /topic/room/{joinCode}/gameState 로 MiniGameStateMessage 전송
                    """
    )
    @MessageResponse(
            path = "/topic/room/{joinCode}/round, /topic/room/{joinCode}/gameState",
            returnType = Object.class
    )
    public void commandGame(@DestinationVariable String joinCode, @Payload MiniGameMessage command) {
        MiniGameCommand miniGameCommand = command.toCommand(objectMapper);
        miniGameCommandDispatcher.dispatch(joinCode, miniGameCommand);
    }
}
