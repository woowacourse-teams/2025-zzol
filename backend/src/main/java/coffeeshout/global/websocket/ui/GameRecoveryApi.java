package coffeeshout.global.websocket.ui;

import coffeeshout.global.websocket.ui.dto.RecoveryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "GameRecovery", description = "게임 복구 API")
public interface GameRecoveryApi {

    @Operation(summary = "메시지 복구 요청", description = "웹소켓 연결이 끊어진 동안 유실된 메시지를 복구합니다.")
    ResponseEntity<RecoveryResponse> requestRecovery(
            @Parameter(description = "방 입장 코드", required = true) @PathVariable @NotBlank String joinCode,
            @Parameter(description = "플레이어 이름", required = true) @RequestParam @NotBlank String playerName,
            @Parameter(description = "마지막으로 수신한 메시지 ID (Redis Stream ID)", required = true) @RequestParam @NotBlank String lastId
    );
}
