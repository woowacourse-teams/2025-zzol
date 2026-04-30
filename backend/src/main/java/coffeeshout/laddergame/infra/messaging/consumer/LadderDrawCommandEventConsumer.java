package coffeeshout.laddergame.infra.messaging.consumer;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.laddergame.application.LadderService;
import coffeeshout.laddergame.domain.event.LadderDrawCommandEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LadderDrawCommandEventConsumer implements Consumer<LadderDrawCommandEvent> {

    private final LadderService ladderService;

    @Override
    public void accept(LadderDrawCommandEvent event) {
        try {
            ladderService.drawLine(event.joinCode(), event.playerName(), event.segmentIndex());
        } catch (BusinessException e) {
            // LadderCommandService에서 이미 warn 로그를 남기므로 Consumer는 debug만 기록
            log.debug("사다리 선 그리기 검증 실패 — 무시: joinCode={}, playerName={}, eventId={}",
                    event.joinCode(), event.playerName(), event.eventId());
        } catch (Exception e) {
            // Redis Stream 재처리를 위해 예외를 전파한다
            log.error("사다리게임 선 그리기 이벤트 처리 중 오류 발생: joinCode={}, playerName={}, segmentIndex={}, eventId={}",
                    event.joinCode(), event.playerName(), event.segmentIndex(), event.eventId(), e);
            throw e;
        }
    }
}
