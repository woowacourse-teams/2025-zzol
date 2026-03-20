package coffeeshout.room.ui;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.ui.request.RoomEnterRequest;
import coffeeshout.room.ui.response.GuestNameExistResponse;
import coffeeshout.room.ui.response.JoinCodeExistResponse;
import coffeeshout.room.ui.response.ProbabilityResponse;
import coffeeshout.room.ui.response.RoomCreateResponse;
import coffeeshout.room.ui.response.RoomEnterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.ResponseEntity;

@Tag(name = "Room", description = "방 관련 API")
public interface RoomApi {

    @Operation(summary = "방 생성", description = "새로운 방을 생성합니다.")
    ResponseEntity<RoomCreateResponse> createRoom(RoomEnterRequest request);

    @Operation(summary = "방 참가", description = "joinCode를 사용하여 방에 참가합니다.")
    CompletableFuture<ResponseEntity<RoomEnterResponse>> enterRoom(
            @Parameter(description = "방 입장 코드", required = true) String joinCode,
            RoomEnterRequest request
    );

    @Operation(summary = "방 코드 존재 확인", description = "joinCode가 유효한지 확인합니다.")
    ResponseEntity<JoinCodeExistResponse> checkJoinCode(
            @Parameter(description = "방 입장 코드", required = true) String joinCode
    );

    @Operation(summary = "게스트 이름 중복 확인", description = "방 내에서 게스트 이름이 중복되는지 확인합니다.")
    ResponseEntity<GuestNameExistResponse> checkGuestName(
            @Parameter(description = "방 입장 코드", required = true) String joinCode,
            @Parameter(description = "게스트 이름", required = true) String guestName
    );

    @Operation(summary = "확률 조회", description = "방의 모든 플레이어 당첨 확률을 조회합니다.")
    ResponseEntity<List<ProbabilityResponse>> getProbabilities(
            @Parameter(description = "방 입장 코드", required = true) String joinCode
    );

    @Operation(summary = "미니게임 전체 목록 조회", description = "사용 가능한 모든 미니게임 목록을 조회합니다.")
    ResponseEntity<List<MiniGameType>> getMiniGames();

    @Operation(summary = "선택된 미니게임 조회", description = "특정 방에서 선택된 미니게임 목록을 조회합니다.")
    ResponseEntity<List<MiniGameType>> getSelectedMiniGames(
            @Parameter(description = "방 입장 코드", required = true) String joinCode
    );

    @Operation(summary = "플레이어 강퇴", description = "방에서 특정 플레이어를 강퇴합니다.")
    ResponseEntity<Void> kickPlayer(
            @Parameter(description = "방 입장 코드", required = true) String joinCode,
            @Parameter(description = "플레이어 이름", required = true) String playerName
    );
}
