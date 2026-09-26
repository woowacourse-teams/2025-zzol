package coffeeshout.user.ui;

import coffeeshout.user.domain.AuthenticatedUser;
import coffeeshout.user.ui.response.MemberRecordsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import org.springframework.http.ResponseEntity;

@Tag(name = "User", description = "회원 API")
public interface UserApi {

    @Operation(summary = "내 기록 조회", description = "로그인 회원의 룰렛 통계(당첨·연속 생존·참여 판수)와 미니게임 기록을 조회합니다. 비로그인이면 401.")
    ResponseEntity<MemberRecordsResponse> getRecords(@Parameter(hidden = true) Optional<AuthenticatedUser> authUser);
}
