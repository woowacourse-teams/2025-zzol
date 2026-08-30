package coffeeshout.wormgame.ui.response;

import coffeeshout.wormgame.domain.WormPosition;
import coffeeshout.wormgame.domain.event.WormsMovedEvent;
import java.util.List;

/** 틱 델타 — 클라는 tick이 단조증가가 아니면 폐기하고, 머리를 append해 궤적을 재구성한다. */
public record WormsStateResponse(long tick, double radius, List<WormPosition> worms) {

    public static WormsStateResponse from(WormsMovedEvent event) {
        return new WormsStateResponse(event.tick(), event.radius(), event.worms());
    }
}
