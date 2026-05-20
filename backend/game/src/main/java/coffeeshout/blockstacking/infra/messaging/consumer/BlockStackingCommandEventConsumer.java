package coffeeshout.blockstacking.infra.messaging.consumer;

import coffeeshout.blockstacking.application.BlockStackingService;
import coffeeshout.blockstacking.domain.event.BlockStackingCommandEvent;
import coffeeshout.exception.custom.BusinessException;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlockStackingCommandEventConsumer implements Consumer<BlockStackingCommandEvent> {

    private final BlockStackingService blockStackingService;

    @Override
    public void accept(BlockStackingCommandEvent event) {
        try {
            blockStackingService.recordProgress(event.joinCode(), event.playerName(), event.floor(),
                    event.movingBlockX(), event.stackTopX(), event.stackTopWidth());
        } catch (BusinessException e) {
            log.warn(
                    "블록 쌓기 명령 이벤트 처리 중 비즈니스 예외 발생: joinCode={}, playerName={}, floor={}, movingBlockX={}, stackTopX={}, stackTopWidth={}",
                    event.joinCode(), event.playerName(), event.floor(),
                    event.movingBlockX(), event.stackTopX(), event.stackTopWidth(), e);
        } catch (Exception e) {
            log.error(
                    "블록 쌓기 명령 이벤트 처리 중 오류 발생: joinCode={}, playerName={}, floor={}, movingBlockX={}, stackTopX={}, stackTopWidth={}",
                    event.joinCode(), event.playerName(), event.floor(),
                    event.movingBlockX(), event.stackTopX(), event.stackTopWidth(), e);
            throw e;
        }
    }
}
