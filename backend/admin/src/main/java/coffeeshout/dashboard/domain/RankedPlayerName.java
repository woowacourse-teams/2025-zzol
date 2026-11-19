package coffeeshout.dashboard.domain;

import java.util.Set;

/**
 * 미니게임 랭킹 행의 플레이어 이름이다. 검열을 통과하지 않은 이름은 여기서 가린다(#1906).
 *
 * <p>응답 record가 원문 문자열 대신 이 타입을 받는다. 새 랭킹을 더하면서 가림을 빠뜨리면 컴파일이 막는다.
 */
public record RankedPlayerName(String playerName, boolean masked) {

    public static RankedPlayerName of(String playerName, Set<String> revealable) {
        if (MaskedNickname.isRevealable(playerName, revealable)) {
            return new RankedPlayerName(playerName, false);
        }
        return new RankedPlayerName(MaskedNickname.TEXT, true);
    }
}
