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
            log.warn("사다리게임 선 그리기 이벤트 처리 중 비즈니스 예외 발생: joinCode={}, playerName={}, segmentIndex={}",
                    event.joinCode(), event.playerName(), event.segmentIndex(), e);
        } catch (Exception e) {
            log.error("사다리게임 선 그리기 이벤트 처리 중 오류 발생: joinCode={}, playerName={}, segmentIndex={}",
                    event.joinCode(), event.playerName(), event.segmentIndex(), e);
            throw e;
        }
    }
}
