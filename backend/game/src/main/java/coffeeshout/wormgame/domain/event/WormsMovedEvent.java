package coffeeshout.wormgame.domain.event;

import coffeeshout.wormgame.domain.WormGame;
import coffeeshout.wormgame.domain.WormPosition;
import java.util.List;

/** 매 틱 델타 — 머리만 싣는다(클라가 append로 궤적을 재구성). tick 단조증가가 아니면 클라가 폐기한다. */
public record WormsMovedEvent(String joinCode, long tick, double radius, List<WormPosition> worms) {

    public static WormsMovedEvent of(WormGame game, String joinCode) {
        return new WormsMovedEvent(joinCode, game.getTickCount(), game.currentRadius(), game.positions());
    }
}
