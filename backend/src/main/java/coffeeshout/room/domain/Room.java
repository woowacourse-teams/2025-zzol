package coffeeshout.room.domain;

import static org.springframework.util.Assert.isTrue;
import static org.springframework.util.Assert.state;

import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.global.exception.custom.InvalidStateException;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.menu.SelectedMenu;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.player.Players;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.domain.roulette.ProbabilityCalculator;
import coffeeshout.room.domain.roulette.Roulette;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import lombok.Getter;

@Getter
public class Room {

    private static final int MAXIMUM_GUEST_COUNT = 9;
    private static final int MINIMUM_GUEST_COUNT = 2;

    private final JoinCode joinCode;
    private final Players players;
    private final Queue<Playable> miniGames;
    private final List<Playable> finishedGames;

    private Player host;
    private RoomState roomState;

    public Room(JoinCode joinCode, PlayerName hostName, SelectedMenu selectedMenu) {
        this.joinCode = joinCode;
        this.host = Player.createHost(hostName, selectedMenu);
        this.players = new Players(joinCode.getValue());
        this.roomState = RoomState.READY;
        this.miniGames = new LinkedList<>();
        this.finishedGames = new ArrayList<>();

        join(host);
    }

    public static Room createNewRoom(JoinCode joinCode, PlayerName hostName, SelectedMenu selectedMenu) {
        return new Room(joinCode, hostName, selectedMenu);
    }

    public void joinGuest(PlayerName guestName, SelectedMenu selectedMenu) {
        validateRoomReady();
        validateCanJoin();
        validatePlayerNameNotDuplicate(guestName);
        join(Player.createGuest(guestName, selectedMenu));
    }

    public void addMiniGame(PlayerName hostName, Playable miniGame) {
        isTrue(host.sameName(hostName), "호스트가 아닙니다.");
        state(miniGames.size() <= 5, "미니게임은 5개 이하여야 합니다.");
        miniGames.add(miniGame);
    }

    public void removeMiniGame(PlayerName hostName, Playable miniGame) {
        isTrue(host.sameName(hostName), "호스트가 아닙니다.");
        isTrue(miniGames.stream().anyMatch(m -> m.getMiniGameType() == miniGame.getMiniGameType()), "미니게임이 존재하지 않습니다.");
        miniGames.removeIf(m -> m.getMiniGameType() == miniGame.getMiniGameType());
    }

    public void applyMiniGameResult(MiniGameResult miniGameResult) {
        ProbabilityCalculator probabilityCalculator = new ProbabilityCalculator(
                players.getPlayerCount(),
                calculateMiniGameCount()
        );
        this.roomState = RoomState.SCORE_BOARD;
        players.adjustProbabilities(miniGameResult, probabilityCalculator);
    }

    private int calculateMiniGameCount() {
        return miniGames.size() + finishedGames.size();
    }

    public boolean isPlayingState() {
        return roomState == RoomState.PLAYING;
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

    public Player findPlayer(PlayerName playerName) {
        return players.getPlayer(playerName);
    }

    public boolean hasPlayer(PlayerName playerName) {
        return players.existsByName(playerName);
    }

    private void join(Player player) {
        players.join(player);
    }

    public List<Playable> getAllMiniGame() {
        return List.copyOf(miniGames);
    }

    public List<MiniGameType> getSelectedMiniGameTypes() {
        return getAllMiniGame().stream()
                .map(Playable::getMiniGameType)
                .toList();
    }

    public Playable findMiniGame(MiniGameType miniGameType) {
        return finishedGames.stream()
                .filter(minigame -> minigame.getMiniGameType() == miniGameType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당하는 미니게임이 존재하지 않습니다."));
    }

    public Playable nextGame() {
        return miniGames.peek();
    }

    public Playable startNextGame(String hostName) {
        state(host.sameName(new PlayerName(hostName)), "호스트가 게임을 시작할 수 있습니다.");
        state(players.isAllReady(), "모든 플레이어가 준비완료해야합니다.");
        state(players.getPlayerCount() >= 2, "게임을 시작하려면 플레이어가 2명 이상이어야 합니다.");
        state(!miniGames.isEmpty(), "시작할 게임이 없습니다.");
        state(isPlayableState(), "게임을 시작할 수 있는 상태가 아닙니다.");

        Playable currentGame = miniGames.poll();

        currentGame.setUp(players.getPlayers());

        roomState = RoomState.PLAYING;

        finishedGames.add(currentGame);

        return currentGame;
    }

    private boolean isPlayableState() {
        return roomState == RoomState.READY || roomState == RoomState.ROULETTE;
    }

    public void updateRouletteState() {
        this.roomState = RoomState.ROULETTE;
    }

    public void updateDoneState() {
        this.roomState = RoomState.DONE;
    }

    public void clearMiniGames() {
        this.miniGames.clear();
    }

    public boolean hasDuplicatePlayerName(PlayerName guestName) {
        return players.hasDuplicateName(guestName);
    }

    public boolean removePlayer(PlayerName playerName) {
        if (players.existsByName(playerName)) {
            players.removePlayer(playerName);

            // 호스트가 나간 경우 새로운 호스트 지정
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
            throw new InvalidArgumentException(RoomErrorCode.QR_CODE_GENERATION_FAILED,
                    "QR 코드는 null일 수 없습니다.");
        }

        joinCode.assignQrCode(qrCode);
    }

    public void showRoulette() {
        this.roomState = RoomState.ROULETTE;
    }

    public boolean isFirstStarted() {
        return finishedGames.size() == 1;
    }

    private boolean hasEnoughPlayers() {
        return players.hasEnoughPlayers(MINIMUM_GUEST_COUNT, MAXIMUM_GUEST_COUNT);
    }

    private boolean canJoin() {
        return players.getPlayerCount() < MAXIMUM_GUEST_COUNT;
    }

    private void validateRoomReady() {
        if (roomState != RoomState.READY) {
            throw new InvalidStateException(
                    RoomErrorCode.ROOM_NOT_READY_TO_JOIN,
                    "READY 상태에서만 참여 가능합니다. 현재 상태: " + roomState
            );
        }
    }

    private void validateCanJoin() {
        if (!canJoin()) {
            throw new InvalidStateException(
                    RoomErrorCode.ROOM_FULL,
                    "방에는 최대 9명만 입장가능합니다. 현재 인원: " + players.getPlayerCount()
            );
        }
    }

    private void validatePlayerNameNotDuplicate(PlayerName guestName) {
        if (hasDuplicatePlayerName(guestName)) {
            throw new InvalidStateException(
                    RoomErrorCode.DUPLICATE_PLAYER_NAME,
                    "중복된 닉네임은 들어올 수 없습니다. 닉네임: " + guestName.value()
            );
        }
    }

    private void promoteNewHost() {
        final Player newHost = players.getFirstPlayer();
        newHost.promote();
        this.host = newHost;
    }
}
