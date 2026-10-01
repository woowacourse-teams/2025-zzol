package coffeeshout.admin.user.domain;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 유저 목록 한 줄.
 *
 * <p>{@link UserSummary} 에 활동량을 더한 것이다. 같은 레코드에 담지 않은 이유는 상세
 * 조회가 활동량을 {@link UserActivity} 로 따로 받고 있어서다. 합치면 상세 화면이 같은
 * 숫자를 두 경로로 들고 있게 된다.
 *
 * <p>담는 것은 <b>목록에서 판단이 끝나는 것</b>이다. 문의 대응에서 먼저 묻는 것은 "이 사람이
 * 얼마나, 무엇을, 언제까지 했나"이지 당첨 횟수가 아니다. 당첨은 룰렛 확률의 결과라 그
 * 사람에 대해 말해 주는 것이 거의 없어 뺐다.
 *
 * <p>{@code createdAt} 과 {@code lastPlayedAt} 의 타입이 다르다. {@code app_user.created_at}
 * 은 Instant 컬럼이고 {@code player.created_at} 은 LocalDateTime 이다. 여기서 억지로
 * 맞추면 어느 한쪽에 시간대 변환이 숨는다.
 *
 * @param playCount    미니게임을 끝낸 횟수. 방에 들어오기만 한 것은 세지 않는다
 * @param topGame      가장 많이 한 미니게임. 한 판도 안 했으면 null
 * @param lastPlayedAt 마지막으로 방에 들어온 때. 활동을 멈춘 사람을 찾는 데 쓴다
 */
public record UserListRow(
        Long id,
        String userCode,
        String nickname,
        Instant createdAt,
        long playCount,
        String topGame,
        LocalDateTime lastPlayedAt) {}
