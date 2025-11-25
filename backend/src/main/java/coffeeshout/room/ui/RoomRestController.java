package coffeeshout.room.ui;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.application.RoomService;
import coffeeshout.room.domain.Playable;
import coffeeshout.room.domain.Room;
import coffeeshout.room.ui.request.RoomEnterRequest;
import coffeeshout.room.ui.response.GuestNameExistResponse;
import coffeeshout.room.ui.response.JoinCodeExistResponse;
import coffeeshout.room.ui.response.ProbabilityResponse;
import coffeeshout.room.ui.response.RemainingMiniGameResponse;
import coffeeshout.room.ui.response.RoomCreateResponse;
import coffeeshout.room.ui.response.RoomEnterResponse;
import jakarta.validation.Valid;
import java.util.List;
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

    @GetMapping("/{joinCode}/miniGames/remaining")
    public ResponseEntity<RemainingMiniGameResponse> getRemainingMiniGames(@PathVariable String joinCode) {
        final List<Playable> miniGames = roomService.getRemainingMiniGames(joinCode);

        return ResponseEntity.ok(RemainingMiniGameResponse.from(miniGames));
    }

    @PostMapping
    public ResponseEntity<RoomCreateResponse> createRoom(@Valid @RequestBody RoomEnterRequest request) {
        final Room room = roomService.createRoom(request.playerName(), request.menu());

        return ResponseEntity.ok(RoomCreateResponse.from(room));
    }

    @PostMapping("/{joinCode}")
    public CompletableFuture<ResponseEntity<RoomEnterResponse>> enterRoom(
            @PathVariable String joinCode,
            @Valid @RequestBody RoomEnterRequest request
    ) {
        return roomService.enterRoomAsync(joinCode, request.playerName(), request.menu())
                .thenApply(room -> ResponseEntity.ok(RoomEnterResponse.from(room)))
                .exceptionally(throwable -> {
                    // 원래 예외 추출
                    final Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                    if (cause instanceof RuntimeException runtimeException) {
                        throw runtimeException;
                    }
                    throw new RuntimeException("방 참가 실패", cause);
                });
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
        final boolean exists = roomService.kickPlayer(joinCode, playerName);

        if (exists) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }
}
