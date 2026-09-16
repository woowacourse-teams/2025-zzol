package coffeeshout.admin.user.ui;

import coffeeshout.admin.support.PageResponse;
import coffeeshout.admin.user.application.UserLookupService;
import coffeeshout.admin.user.application.UserLookupService.ProviderStats;
import coffeeshout.admin.user.ui.response.ProviderStatsResponse;
import coffeeshout.admin.user.ui.response.UserDetailResponse;
import coffeeshout.admin.user.ui.response.UserStatsResponse;
import coffeeshout.admin.user.ui.response.UserSummaryResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 유저 조회.
 *
 * <p><b>활성 회원만 나온다.</b> {@code UserEntity}의 {@code @SQLRestriction("deleted_at IS NULL")}이
 * 모든 JPA 조회에 그 조건을 덧붙이기 때문이다. 탈퇴 회원 조회가 필요해지면 네이티브 쿼리
 * 경로를 따로 열어야 한다.
 *
 * <p>조회만 있다. 강제 탈퇴 같은 조치는 넣지 않았다. 지금 백오피스에 그 요구가 없고,
 * 되돌릴 수 없는 기능은 필요해질 때 그 맥락에서 설계하는 편이 낫다.
 */
@RestController
@RequestMapping("/admin/api/users")
@Validated
@RequiredArgsConstructor
public class AdminUserController {

    private final UserLookupService userLookupService;

    /**
     * @param keyword 닉네임 부분 일치 또는 유저코드 완전 일치
     */
    @GetMapping
    public PageResponse<UserSummaryResponse.Row> search(
            @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return PageResponse.of(userLookupService.search(keyword, page), UserSummaryResponse.Row::from);
    }

    /**
     * 소셜 제공자 분포.
     *
     * <p>{@code /{userId}} 보다 먼저 선언한다. 순서로 결정되지는 않지만, 읽는 사람이
     * 리터럴 경로와 변수 경로가 같은 자리에 있다는 것을 바로 보게 하려는 것이다.
     * 매칭은 스프링이 리터럴을 더 구체적인 패턴으로 보고 고른다.
     */
    @GetMapping("/providers")
    public ProviderStatsResponse providers() {
        final ProviderStats stats = userLookupService.findProviderStats();
        return ProviderStatsResponse.of(stats.userCount(), stats.counts());
    }

    /**
     * 화면 상단 그래프.
     *
     * <p>{@code days} 는 가입 추이에만 걸린다. 참여도와 잔존 분포는 회원 전체가 대상이다.
     * 기간을 걸면 그 기간에 활동한 사람만 남아, 빠져나간 사람을 묻는 그래프에서 빠져나간
     * 사람이 사라진다.
     */
    @GetMapping("/stats")
    public UserStatsResponse stats(@RequestParam(defaultValue = "30") @Min(1) @Max(180) int days) {
        return UserStatsResponse.from(userLookupService.findStats(days));
    }

    @GetMapping("/{userId}")
    public UserDetailResponse detail(@PathVariable Long userId) {
        return UserDetailResponse.from(userLookupService.findDetail(userId));
    }
}
