package coffeeshout.admin.user.ui.response;

import coffeeshout.admin.user.application.UserLookupService.UserDetail;
import java.util.List;

/**
 * @param providers 연결된 소셜 제공자. 여러 개일 수 있다.
 * @param winRate   참여 대비 당첨 비율. "왜 나만 걸리냐" 문의에 답하는 값이다.
 */
public record UserDetailResponse(
        UserSummaryResponse summary, List<String> providers, long roomCount, long winCount, double winRate) {

    public static UserDetailResponse from(UserDetail detail) {
        return new UserDetailResponse(
                UserSummaryResponse.from(detail.summary()),
                detail.providers(),
                detail.activity().roomCount(),
                detail.activity().winCount(),
                detail.activity().winRate());
    }
}
