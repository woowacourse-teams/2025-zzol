package coffeeshout.admin.room.ui.response;

import coffeeshout.admin.room.application.RoomLookupService.RoomDetail;
import java.util.List;

/**
 * @param roulette 룰렛까지 못 간 방은 null 이다. 중도 이탈은 정상 경로다.
 */
public record RoomDetailResponse(
        RoomSummaryResponse summary,
        List<RoomPlayerResponse> players,
        List<RoomMiniGameResultResponse> miniGameResults,
        RoomRouletteResultResponse roulette) {

    public static RoomDetailResponse from(RoomDetail detail) {
        return new RoomDetailResponse(
                RoomSummaryResponse.from(detail.summary()),
                detail.players().stream().map(RoomPlayerResponse::from).toList(),
                detail.miniGameResults().stream()
                        .map(RoomMiniGameResultResponse::from)
                        .toList(),
                detail.rouletteResult().map(RoomRouletteResultResponse::from).orElse(null));
    }
}
