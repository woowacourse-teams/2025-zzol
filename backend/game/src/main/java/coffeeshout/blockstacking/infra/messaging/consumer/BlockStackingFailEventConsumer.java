package coffeeshout.blockstacking.infra.messaging.consumer;

import coffeeshout.blockstacking.application.BlockStackingService;
import coffeeshout.blockstacking.domain.event.BlockStackingFailEvent;
import coffeeshout.exception.custom.BusinessException;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlockStackingFailEventConsumer implements Consumer<BlockStackingFailEvent> {

    private final BlockStackingService blockStackingService;

    @Override
    public void accept(BlockStackingFailEvent event) {
        try {
            blockStackingService.recordFailure(event.joinCode(), event.playerName());
        } catch (BusinessException e) {
            log.warn(
                    "블록 쌓기 실패 이벤트 처리 중 비즈니스 예외 발생: joinCode={}, playerName={}",
                    event.joinCode(), event.playerName(), e);
        } catch (Exception e) {
            log.error(
                    "블록 쌓기 실패 이벤트 처리 중 오류 발생: joinCode={}, playerName={}",
                    event.joinCode(), event.playerName(), e);
            throw e;
        }
    }
}
