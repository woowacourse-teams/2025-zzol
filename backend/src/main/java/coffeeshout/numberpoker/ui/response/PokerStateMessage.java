package coffeeshout.numberpoker.ui.response;

import coffeeshout.numberpoker.domain.NumberPokerGame;
import coffeeshout.numberpoker.domain.PokerCard;
import coffeeshout.numberpoker.domain.PokerPhase;
import coffeeshout.numberpoker.domain.PokerRoundResult;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import java.util.List;
import java.util.Map;

public record PokerStateMessage(
        String phase,
        int roundNumber,
        int totalRounds,
        Integer timerSeconds,      // STAGE_1·STAGE_2·ROUND_READY 에서만 값 존재, 나머지는 null
        List<Integer> dealerCards, // 공개된 딜러 카드
        int dealerHiddenCount,     // 뒷면(미공개) 딜러 카드 수
        List<PlayerInfo> players
) {

    public record PlayerInfo(
            String playerName,
            boolean folded,
            boolean ready,
            String result,           // SHOWDOWN·SCORE_BOARD 페이즈에서만 값 존재, 나머지는 null
            Integer probability,
            Integer probabilityDelta
    ) {
    }

    /** 타이머가 있는 페이즈 전환용 (STAGE_1·STAGE_2·ROUND_READY 및 타이머 없는 페이즈 공통) */
    public static PokerStateMessage from(NumberPokerGame game, Room room, Integer timerSeconds) {
        return build(game, room, timerSeconds, Map.of());
    }

    /** SCORE_BOARD 전용 — 확률 변동량 포함, 타이머 없음 */
    public static PokerStateMessage from(NumberPokerGame game, Room room, Map<Player, Integer> probabilityDeltas) {
        return build(game, room, null, probabilityDeltas);
    }

    private static PokerStateMessage build(NumberPokerGame game, Room room,
                                           Integer timerSeconds,
                                           Map<Player, Integer> probabilityDeltas) {
        final String phase = game.getCurrentPhase() != null ? game.getCurrentPhase().name() : null;

        final List<Integer> dealerCards = game.getDealerVisibleCards().stream()
                .map(PokerCard::value)
                .toList();
        final int dealerHiddenCount = game.getDealerHiddenCount();

        // result는 SHOWDOWN·SCORE_BOARD에서만 포함 — 그 이전 페이즈에 노출되면 게임 정보가 유출됨
        final boolean showResult = game.getCurrentPhase() == PokerPhase.SHOWDOWN
                || game.getCurrentPhase() == PokerPhase.SCORE_BOARD;
        final Map<Player, PokerRoundResult> results = showResult && game.hasCurrentRound()
                ? game.getCurrentRoundResults()
                : Map.of();

        final List<PlayerInfo> playerInfos = room.getPlayers().stream()
                .map(player -> new PlayerInfo(
                        player.getName().value(),
                        game.hasCurrentRound() && game.isPlayerFolded(player),
                        game.isPlayerReady(player),
                        results.containsKey(player) ? results.get(player).name() : null,
                        player.getProbability() != null ? player.getProbability().value() : null,
                        probabilityDeltas.getOrDefault(player, null)
                ))
                .toList();

        return new PokerStateMessage(phase, game.getCurrentRoundNumber(), game.getTotalRounds(),
                timerSeconds, dealerCards, dealerHiddenCount, playerInfos);
    }
}
