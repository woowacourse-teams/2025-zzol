package coffeeshout.gamecommon.flow;

import java.time.Duration;
import java.util.function.Consumer;

public interface FlowHandle {

    FlowHandle andThen(Runnable action, Duration delay);

    /**
     * 이 핸들이 완료된 시점부터 timeout과 trigger를 경쟁시킵니다. - trigger가 먼저 완료되면 earlyFinishExtraDelay 후 진행 - timeout이 먼저 완료되면 즉시 진행
     */
    FlowHandle raceTimeout(Duration timeout, EarlyFinishTrigger trigger, Duration earlyFinishExtraDelay);

    FlowHandle onError(Consumer<Throwable> errorHandler);
}
