package coffeeshout.gamecommon;

import java.util.List;
import java.util.Optional;

/**
 * 시즌 리더보드 표시용 회원 프로필 조회 포트. {@code :game}이 정의하고 {@code :user}가 구현한다 —
 * {@code RoomSnapshotQuery}(ADR-0034)와 같은 역전 패턴으로, {@code :game→:user} 의존을 만들지 않는다.
 * <p>
 * 외부 노출 식별자는 ADR-0024의 전역 식별({@code nickname#userCode})을 따른다. userId는
 * 정산 내부 키로만 쓰고 API 응답에 노출하지 않는다(#1610).
 */
public interface SeasonUserProfileQuery {

    /** 리더보드에 올릴 회원들의 표시 프로필. 탈퇴 회원은 결과에서 빠질 수 있다. */
    List<SeasonUserProfile> resolveProfiles(List<Long> userIds);

    /** 전역 식별자의 userCode로 내부 userId를 해석한다("내 순위" 조회 진입점). */
    Optional<Long> resolveUserIdByCode(String userCode);

    record SeasonUserProfile(Long userId, String nickname, String userCode) {
    }
}
