package coffeeshout.room.domain;

import static org.springframework.util.Assert.isTrue;
import static org.springframework.util.Assert.state;

import coffeeshout.exception.GlobalErrorCode;
import coffeeshout.exception.custom.BusinessException;
import coffeeshout.exception.custom.SystemException;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.player.Players;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.domain.roulette.ProbabilityCalculator;
import coffeeshout.room.domain.roulette.Roulette;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class Room {

    private static final int MAXIMUM_GUEST_COUNT = 9;
    private static final int MINIMUM_GUEST_COUNT = 2;

    private final JoinCode joinCode;
    private final Players players;

    private Player host;
    private RoomState roomState;
    private double adjustmentWeight;

    public Room(JoinCode joinCode, PlayerName hostName, double adjustmentWeight) {
        this(joinCode, hostName, null, adjustmentWeight);
    }

    public Room(JoinCode joinCode, PlayerName hostName, Long userId, double adjustmentWeight) {
        validateAdjustmentWeight(adjustmentWeight);
        this.joinCode = joinCode;
        this.host = Player.createHost(hostName, userId);
        this.players = new Players(joinCode.getValue());
        this.roomState = RoomState.READY;
        this.adjustmentWeight = adjustmentWeight;

        join(host);
    }

    public static Room createNewRoom(JoinCode joinCode, PlayerName hostName, Long userId, double adjustmentWeight) {
        return new Room(joinCode, hostName, userId, adjustmentWeight);
    }

    public void joinGuest(PlayerName guestName) {
        joinGuest(guestName, null);
    }

    public void joinGuest(PlayerName guestName, Long userId) {
        validateRoomReady();
        if (userId != null && players.existsByUserId(userId)) {
            players.removePlayerByUserId(userId);
        } else {
            validateCanJoin();
            validatePlayerNameNotDuplicate(guestName);
        }
        join(Player.createGuest(guestName, userId));
    }

    public void markPlaying(PlayerName hostName) {
        state(host.sameName(hostName), "호스트가 게임을 시작할 수 있습니다.");
        state(players.isAllReady(), "모든 플레이어가 준비 완료해야합니다.");
        state(players.getPlayerCount() >= 2, "게임을 시작하려면 플레이어가 2명 이상이어야 합니다.");
        state(isPlayableState(), "게임을 시작할 수 있는 상태가 아닙니다.");
        roomState = RoomState.PLAYING;
    }

    public void applyGameResult(Map<PlayerName, Integer> rankByPlayer, int totalGameCount) {
        final ProbabilityCalculator probabilityCalculator = new ProbabilityCalculator(
                players.getPlayerCount(),
                totalGameCount,
                adjustmentWeight
        );
        this.roomState = RoomState.SCORE_BOARD;
        players.adjustProbabilities(rankByPlayer, probabilityCalculator);
    }

    public void updateAdjustmentWeight(PlayerName hostName, double adjustmentWeight) {
        validateHost(hostName);
        validateRoomUpdatable();
        validateAdjustmentWeight(adjustmentWeight);
        this.adjustmentWeight = adjustmentWeight;
    }

    public Winner spinRoulette(Player host, Roulette roulette) {
        isTrue(isHost(host), "호스트만 룰렛을 돌릴 수 있습니다.");
        state(hasEnoughPlayers(), "룰렛은 2~9명의 플레이어가 참여해야 시작할 수 있습니다.");
        state(roomState == RoomState.ROULETTE, "게임이 끝나야만 룰렛을 돌릴 수 있습니다.");
        final Winner winner = roulette.spin(players);
        roomState = RoomState.DONE;
        return winner;
    }

    public boolean isHost(Player player) {
        return host.equals(player);
    }

    public List<Player> getPlayers() {
        return players.getPlayers();
    }

    public Map<PlayerName, Integer> toColorIndexMap() {
        return players.getPlayers().stream()
                .collect(Collectors.toUnmodifiableMap(Player::getName, Player::getColorIndex));
    }

    public Player findPlayer(PlayerName playerName) {
        return players.getPlayer(playerName);
    }

    private void join(Player player) {
        players.join(player);
    }

    public boolean removePlayer(PlayerName playerName) {
        if (players.existsByName(playerName)) {
            players.removePlayer(playerName);

            if (host.sameName(playerName) && !players.isEmpty()) {
                promoteNewHost();
            }

            return true;
        }

        return false;
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    public boolean isReadyState() {
        return roomState == RoomState.READY;
    }

    public void assignQrCode(QrCode qrCode) {
        if (qrCode == null) {
            throw new SystemException(GlobalErrorCode.INTERNAL_SERVER_ERROR,
                    "QR 코드는 null일 수 없습니다.");
        }

        joinCode.assignQrCode(qrCode);
    }

    public void showRoulette() {
        this.roomState = RoomState.ROULETTE;
    }

    public boolean hasDuplicatePlayerName(PlayerName guestName) {
        return players.hasDuplicateName(guestName);
    }

    private boolean isPlayableState() {
        return roomState == RoomState.READY || roomState == RoomState.ROULETTE;
    }

    private boolean hasEnoughPlayers() {
        return players.hasEnoughPlayers(MINIMUM_GUEST_COUNT, MAXIMUM_GUEST_COUNT);
    }

    private boolean canJoin() {
        return players.getPlayerCount() < MAXIMUM_GUEST_COUNT;
    }

    private void validateRoomReady() {
        if (roomState != RoomState.READY) {
            throw new BusinessException(
                    RoomErrorCode.ROOM_NOT_READY_TO_JOIN,
                    "READY 상태에서만 참여 가능합니다. 현재 상태: " + roomState
            );
        }
    }

    private void validateRoomUpdatable() {
        if (roomState != RoomState.READY) {
            throw new BusinessException(
                    RoomErrorCode.ROOM_NOT_READY_TO_UPDATE,
                    "READY 상태에서만 설정을 변경할 수 있습니다. 현재 상태: " + roomState
            );
        }
    }

    private void validateCanJoin() {
        if (!canJoin()) {
            throw new BusinessException(
                    RoomErrorCode.ROOM_FULL,
                    "방에는 최대 9명만 입장 가능합니다. 현재 인원: " + players.getPlayerCount()
            );
        }
    }

    private void validatePlayerNameNotDuplicate(PlayerName guestName) {
        if (hasDuplicatePlayerName(guestName)) {
            throw new BusinessException(
                    RoomErrorCode.DUPLICATE_PLAYER_NAME,
                    "중복된 닉네임은 들어올 수 없습니다. 닉네임: " + guestName.value()
            );
        }
    }

    private void validateHost(PlayerName hostName) {
        if (!host.sameName(hostName)) {
            throw new BusinessException(RoomErrorCode.NOT_HOST, "호스트만 가중치를 변경할 수 있습니다.");
        }
    }

    private void validateAdjustmentWeight(double adjustmentWeight) {
        if (adjustmentWeight < 0.1 || adjustmentWeight > 0.9) {
            throw new BusinessException(
                    RoomErrorCode.INVALID_ADJUSTMENT_WEIGHT,
                    "가중치는 0.1 이상 0.9 이하여야 합니다. 입력값: " + adjustmentWeight
            );
        }
    }

    private void promoteNewHost() {
        final Player newHost = players.getFirstPlayer();
        newHost.promote();
        this.host = newHost;
    }
}
