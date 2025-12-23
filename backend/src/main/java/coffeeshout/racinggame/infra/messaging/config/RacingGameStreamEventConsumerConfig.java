package coffeeshout.racinggame.infra.messaging.config;

import coffeeshout.global.exception.custom.InvalidStateException;
import coffeeshout.racinggame.application.RacingGameService;
import coffeeshout.racinggame.domain.event.TapCommandEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RacingGameStreamEventConsumerConfig {

    private final RacingGameService racingGameService;

    @Bean
    public Consumer<TapCommandEvent> tapCommandEventConsumer() {
        return event -> {
            try {
                racingGameService.tap(
                        event.joinCode(),
                        event.playerName(),
                        event.tapCount()
                );
            } catch (InvalidStateException e) {
                log.warn("탭 이벤트 처리 중 상태 오류: eventId={}, joinCode={}",
                        event.eventId(), event.joinCode(), e);
            } catch (Exception e) {
                log.error("탭 이벤트 처리 실패: eventId={}, joinCode={}",
                        event.eventId(), event.joinCode(), e);
            }
        };
    }
}
