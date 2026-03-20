package coffeeshout.minigame.domain;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@Slf4j
public class MiniGameTask implements Runnable {

    private final Runnable task;
    private final String correlationId;

    public MiniGameTask(Runnable task, String correlationId) {
        this.task = task;
        this.correlationId = correlationId;
    }

    @Override
    public void run() {
        try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", correlationId)) {
            task.run();
        } catch (Exception e) {
            log.error("태스크 실행 중 예외 발생, correlationId={}", correlationId, e);
            throw e;
        }
    }
}
