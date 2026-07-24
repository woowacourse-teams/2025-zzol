package coffeeshout.user.infra.persistence;

import coffeeshout.gamecommon.SeasonUserProfileQuery;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@code :game}이 정의한 시즌 프로필 포트의 {@code :user} 구현(ADR-0034 역전 패턴).
 * UserEntity의 {@code @SQLRestriction}으로 탈퇴 회원은 자동 제외된다 — 리더보드에서
 * 탈퇴 회원 행이 빠지는 것은 의도된 동작이다.
 */
@Component
@RequiredArgsConstructor
public class SeasonUserProfileQueryAdapter implements SeasonUserProfileQuery {

    private final UserJpaRepository userJpaRepository;

    @Override
    public List<SeasonUserProfile> resolveProfiles(List<Long> userIds) {
        return userJpaRepository.findAllById(userIds).stream()
                .map(user -> new SeasonUserProfile(user.getId(), user.getNickname(), user.getUserCode()))
                .toList();
    }

    @Override
    public Optional<Long> resolveUserIdByCode(String userCode) {
        return userJpaRepository.findByUserCode(userCode).map(UserEntity::getId);
    }
}
