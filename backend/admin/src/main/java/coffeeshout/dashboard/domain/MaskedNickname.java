package coffeeshout.dashboard.domain;

import java.util.Set;

/**
 * 검열을 통과하지 않은 랭킹 닉네임 자리에 원문 대신 내보내는 문구다(#1906).
 *
 * <p>{@code null}로 비우지 않는다. 배포 직후 {@code masked}를 모르는 옛 프론트 번들은 이 문구를 그대로 그린다.
 */
public final class MaskedNickname {

    public static final String TEXT = "처리 중인 닉네임";

    private MaskedNickname() {}

    public static boolean isRevealable(String nickname, Set<String> revealable) {
        return nickname != null && revealable.contains(nickname);
    }
}
