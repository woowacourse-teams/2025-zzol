package coffeeshout.admin.room.ui;

import coffeeshout.admin.room.application.RoomLookupService;
import coffeeshout.admin.room.application.RoomStatsService;
import coffeeshout.admin.room.ui.response.RoomDetailResponse;
import coffeeshout.admin.room.ui.response.RoomStatsResponse;
import coffeeshout.admin.room.ui.response.RoomSummaryResponse;
import coffeeshout.admin.support.PageResponse;
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
 * 방 조회와 드릴다운.
 *
 * <p>상세를 {@code joinCode}가 아니라 {@code id}로 연다. join_code 에는 유니크 제약이 없어
 * 같은 코드가 시간이 지나 다른 방에 다시 쓰인다. 코드로 상세를 열면 어느 방인지 결정되지 않는다.
 * 검색은 코드로 하고, 결과에서 방을 골라 들어간다.
 */
@RestController
@RequestMapping("/admin/api/rooms")
@Validated
@RequiredArgsConstructor
public class AdminRoomController {

    private final RoomLookupService roomLookupService;
    private final RoomStatsService roomStatsService;

    @GetMapping
    public PageResponse<RoomSummaryResponse> search(
            @RequestParam(required = false) String joinCode, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return PageResponse.of(roomLookupService.search(joinCode, page), RoomSummaryResponse::from);
    }

    /**
     * 화면 상단 그래프.
     *
     * <p>{@code /{roomId}} 보다 먼저 선언한다. 매칭은 스프링이 리터럴을 더 구체적인 패턴으로
     * 보고 고르지만, 읽는 사람이 두 경로가 같은 자리에 있다는 것을 바로 보게 하려는 것이다.
     *
     * @param days 상한을 둔다. 기간 안의 방을 한 줄씩 읽으므로 기간이 곧 읽는 양이다.
     */
    @GetMapping("/stats")
    public RoomStatsResponse stats(@RequestParam(defaultValue = "30") @Min(1) @Max(90) int days) {
        return RoomStatsResponse.from(roomStatsService.findStats(days));
    }

    @GetMapping("/{roomId}")
    public RoomDetailResponse detail(@PathVariable Long roomId) {
        return RoomDetailResponse.from(roomLookupService.findDetail(roomId));
    }
}
