package coffeeshout.nunchi.infra.messaging.consumer;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.nunchi.application.NunchiService;
import coffeeshout.nunchi.domain.event.NunchiCommandEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 눈치게임 press 입력 컨슈머(ADR-0031 N1). 단일 스트림을 단일스레드로 도착 순서대로 처리하고,
 * 권위 시각({@link NunchiCommandEvent#timestamp})을 서비스에 그대로 넘긴다. 잘못된 입력은 도메인이
 * {@code IGNORED}로 흡수하므로 여기선 비즈니스 예외만 warn 로그로 흘린다(결정 1).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NunchiInputConsumer implements Consumer<NunchiCommandEvent> {

    private final NunchiService nunchiService;

    @Override
    public void accept(NunchiCommandEvent event) {
        try {
            nunchiService.handlePress(event.joinCode(), event.playerName(), event.timestamp());
        } catch (BusinessException e) {
            log.warn("눈치게임 press 처리 중 비즈니스 예외: joinCode={}, playerName={}, eventId={}",
                    event.joinCode(), event.playerName(), event.eventId(), e);
        } catch (Exception e) {
            log.error("눈치게임 press 처리 중 오류: joinCode={}, playerName={}, eventId={}",
                    event.joinCode(), event.playerName(), event.eventId(), e);
            throw e;
        }
    }
}
