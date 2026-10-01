package coffeeshout.admin.user.ui.response;

import coffeeshout.admin.user.domain.UserListRow;
import coffeeshout.admin.user.domain.UserSummary;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 이메일은 없다. 암호화 저장이라 복호화해야 읽히는데 백오피스가 그 열쇠를 쥘 이유가 없다.
 * 사람을 특정하는 데는 유저코드로 충분하다.
 *
 * <p>탈퇴 여부도 없다. {@code @SQLRestriction} 때문에 여기 오는 유저는 전부 활성 회원이라
 * 그 칸을 두면 언제나 같은 값이 찍힌다.
 */
public record UserSummaryResponse(Long id, String userCode, String nickname, Instant createdAt) {

    public static UserSummaryResponse from(UserSummary summary) {
        return new UserSummaryResponse(summary.id(), summary.userCode(), summary.nickname(), summary.createdAt());
    }

    /**
     * 목록 줄에는 활동량이 함께 온다. 상세를 열지 않고도 얼마나 썼는지 보인다.
     *
     * <p>{@code topGame} 은 enum 이름 그대로다. 한글 이름은 화면이 갖는다. 서버가 이름을
     * 붙여 주는 다른 자리(게임별 비중)와 다른 이유는, 여기서는 그 값으로 색이나 아이콘을
     * 고를 일이 없고 오직 글자로만 쓰이기 때문이다.
     */
    public record Row(
            Long id,
            String userCode,
            String nickname,
            Instant createdAt,
            long playCount,
            String topGame,
            LocalDateTime lastPlayedAt) {

        public static Row from(UserListRow row) {
            return new Row(
                    row.id(),
                    row.userCode(),
                    row.nickname(),
                    row.createdAt(),
                    row.playCount(),
                    row.topGame(),
                    row.lastPlayedAt());
        }
    }
}
