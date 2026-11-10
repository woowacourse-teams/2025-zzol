package coffeeshout.dashboard.domain;

import java.util.Set;

/**
 * 당첨자 랭킹 행의 회원이다. 검열을 통과하지 않은 닉네임은 여기서 가린다(#1906).
 *
 * <p>가릴 때는 유저 코드도 비운다. 코드가 남으면 응답만 보고도 가린 닉네임이 누구인지 알 수 있다.
 */
public record RankedMember(String nickname, String userCode, boolean masked) {

    public static RankedMember of(String nickname, String userCode, Set<String> revealable) {
        if (MaskedNickname.isRevealable(nickname, revealable)) {
            return new RankedMember(nickname, userCode, false);
        }
        return new RankedMember(MaskedNickname.TEXT, null, true);
    }
}
