package coffeeshout.room.ui;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.application.service.PlayerService;
import coffeeshout.room.application.service.RoomService;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.Playable;
import coffeeshout.room.domain.Room;
import coffeeshout.room.ui.request.RoomEnterRequest;
import coffeeshout.room.ui.response.GuestNameExistResponse;
import coffeeshout.room.ui.response.JoinCodeExistResponse;
import coffeeshout.room.ui.response.ProbabilityResponse;
import coffeeshout.room.ui.response.RandomNicknameResponse;
import coffeeshout.room.ui.response.RemainingMiniGameResponse;
import coffeeshout.room.ui.response.RoomCreateResponse;
import coffeeshout.room.ui.response.RoomEnterResponse;
import coffeeshout.user.domain.AuthenticatedUser;
import coffeeshout.user.ui.resolver.AuthUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomRestController implements RoomApi {

    private final RoomService roomService;
    private final PlayerService playerService;

    @GetMapping("/{joinCode}/miniGames/remaining")
    public ResponseEntity<RemainingMiniGameResponse> getRemainingMiniGames(@PathVariable String joinCode) {
        final List<Playable> miniGames = roomService.getRemainingMiniGames(joinCode);

        return ResponseEntity.ok(RemainingMiniGameResponse.from(miniGames));
    }

    @PostMapping
    public ResponseEntity<RoomCreateResponse> createRoom(
            @AuthUser Optional<AuthenticatedUser> authUser,
            @RequestBody(required = false) @Valid RoomEnterRequest request
    ) {
        final Room room = authUser.isPresent()
                ? roomService.createRoom(authUser.get())
                : roomService.createRoom(requirePlayerName(request));

        return ResponseEntity.ok(RoomCreateResponse.from(room));
    }

    @PostMapping("/{joinCode}")
    public CompletableFuture<ResponseEntity<RoomEnterResponse>> enterRoom(
            @PathVariable String joinCode,
            @AuthUser Optional<AuthenticatedUser> authUser,
            @RequestBody(required = false) @Valid RoomEnterRequest request
    ) {
        final CompletableFuture<Room> future = authUser.isPresent()
                ? roomService.enterRoomAsync(joinCode, authUser.get())
                : roomService.enterRoomAsync(joinCode, requirePlayerName(request));

        return future
                .thenApply(room -> ResponseEntity.ok(RoomEnterResponse.from(room)))
                .exceptionally(throwable -> {
                    final Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                    if (cause instanceof RuntimeException runtimeException) {
                        throw runtimeException;
                    }
                    throw new RuntimeException("방 참가 실패", cause);
                });
    }

    private String requirePlayerName(RoomEnterRequest request) {
        if (request == null || request.playerName() == null || request.playerName().isBlank()) {
            throw new BusinessException(RoomErrorCode.PLAYER_NAME_BLANK, "플레이어 이름이 없습니다.");
        }
        return request.playerName();
    }

    @GetMapping("/nickname/random")
    public ResponseEntity<RandomNicknameResponse> generateRandomNickname(
            @RequestParam(required = false) String joinCode
    ) {
        final String nickname = (joinCode != null && !joinCode.isBlank())
                ? roomService.generateRandomNicknameForGuest(joinCode)
                : roomService.generateRandomNicknameForHost();

        return ResponseEntity.ok(RandomNicknameResponse.from(nickname));
    }

    @GetMapping("/check-joinCode")
    public ResponseEntity<JoinCodeExistResponse> checkJoinCode(@RequestParam String joinCode) {
        final boolean exists = roomService.roomExists(joinCode);

        return ResponseEntity.ok(JoinCodeExistResponse.from(exists));
    }

    @GetMapping("/check-guestName")
    public ResponseEntity<GuestNameExistResponse> checkGuestName(
            @RequestParam String joinCode,
            @RequestParam String guestName
    ) {
        final boolean isDuplicated = roomService.isGuestNameDuplicated(joinCode, guestName);

        return ResponseEntity.ok(GuestNameExistResponse.from(isDuplicated));
    }

    @GetMapping("/{joinCode}/probabilities")
    public ResponseEntity<List<ProbabilityResponse>> getProbabilities(@PathVariable String joinCode) {
        final List<ProbabilityResponse> responses = roomService.getProbabilities(joinCode);

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/minigames")
    public ResponseEntity<List<MiniGameType>> getMiniGames() {
        final List<MiniGameType> responses = roomService.getAllMiniGames();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/minigames/selected")
    public ResponseEntity<List<MiniGameType>> getSelectedMiniGames(@RequestParam String joinCode) {
        final List<MiniGameType> result = roomService.getSelectedMiniGames(joinCode);

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{joinCode}/players/{playerName}")
    public ResponseEntity<Void> kickPlayer(
            @PathVariable String joinCode,
            @PathVariable String playerName
    ) {
        final boolean exists = playerService.checkAndKickPlayer(joinCode, playerName);

        if (exists) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }
}
