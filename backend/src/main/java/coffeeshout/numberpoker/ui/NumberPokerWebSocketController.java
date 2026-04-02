package coffeeshout.numberpoker.ui;

import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.global.websocket.PlayerKey;
import coffeeshout.numberpoker.infra.messaging.event.FoldCommandEvent;
import coffeeshout.numberpoker.infra.messaging.event.ReadyCommandEvent;
import coffeeshout.numberpoker.infra.messaging.event.SettingsCommandEvent;
import coffeeshout.numberpoker.ui.command.SettingsCommand;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class NumberPokerWebSocketController {

    private final StreamPublisher streamPublisher;

    @MessageMapping("/room/{joinCode}/poker/fold")
    public void fold(@DestinationVariable String joinCode, Principal principal) {
        PlayerKey playerKey = resolveVerifiedPlayer(principal, joinCode);
        if (playerKey == null) {
            return;
        }
        streamPublisher.publish(StreamKey.NUMBER_POKER_EVENTS, new FoldCommandEvent(joinCode, playerKey.playerName()));
        log.debug("폴드 요청: joinCode={}, player={}", joinCode, playerKey.playerName());
    }

    @MessageMapping("/room/{joinCode}/poker/ready")
    public void ready(@DestinationVariable String joinCode, Principal principal) {
        PlayerKey playerKey = resolveVerifiedPlayer(principal, joinCode);
        if (playerKey == null) {
            return;
        }
        streamPublisher.publish(StreamKey.NUMBER_POKER_EVENTS, new ReadyCommandEvent(joinCode, playerKey.playerName()));
        log.debug("레디 요청: joinCode={}, player={}", joinCode, playerKey.playerName());
    }

    @MessageMapping("/room/{joinCode}/poker/settings")
    public void settings(@DestinationVariable String joinCode, @Payload SettingsCommand command, Principal principal) {
        PlayerKey playerKey = resolveVerifiedPlayer(principal, joinCode);
        if (playerKey == null) {
            return;
        }
        streamPublisher.publish(StreamKey.NUMBER_POKER_EVENTS,
                new SettingsCommandEvent(joinCode, playerKey.playerName(), command.roundCount()));
        log.debug("설정 요청: joinCode={}, host={}, roundCount={}",
                joinCode, playerKey.playerName(), command.roundCount());
    }

    private PlayerKey resolveVerifiedPlayer(Principal principal, String joinCode) {
        if (principal == null || !PlayerKey.isValid(principal.getName())) {
            log.warn("인증 정보 없는 WebSocket 요청 거부: principal={}, joinCode={}", principal, joinCode);
            return null;
        }
        PlayerKey playerKey = PlayerKey.parse(principal.getName());
        if (!playerKey.joinCode().equals(joinCode)) {
            log.warn("joinCode 불일치 요청 거부: principal={}, requestedJoinCode={}", principal.getName(), joinCode);
            return null;
        }
        return playerKey;
    }
}
