package coffeeshout.settlement.infra.messaging;

import coffeeshout.settlement.application.SettlementNotifier;
import coffeeshout.settlement.event.SeasonRankUpdatedEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 브로드캐스트 스트림(minigame)으로 되돌아온 순위 변동 이벤트의 소비자.
 * EventDispatcher가 전 인스턴스에서 호출하고, 각 인스턴스는 자기 로컬 세션에 알림을 뿌린다.
 */
@Component
@RequiredArgsConstructor
public class SeasonRankUpdatedEventConsumer implements Consumer<SeasonRankUpdatedEvent> {

    private final SettlementNotifier settlementNotifier;

    @Override
    public void accept(SeasonRankUpdatedEvent event) {
        settlementNotifier.notifyRankUpdated(event);
    }
}
