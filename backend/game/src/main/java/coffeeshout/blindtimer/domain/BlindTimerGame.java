package coffeeshout.blindtimer.domain;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.Playable;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
public class BlindTimerGame implements Playable {

    private static final Duration TARGET_MIN = Duration.ofSeconds(5);
    private static final Duration TARGET_MAX = Duration.ofMillis(19990);

    private BlindTimerPlayers players;
    @Getter(AccessLevel.NONE)
    private final AtomicReference<BlindTimerGameState> state =
            new AtomicReference<>(BlindTimerGameState.DESCRIPTION);
    private volatile Instant startTime;
    private final Duration targetTime;

    @Setter
    private ScheduledFuture<?> timeoutFuture;

    public BlindTimerGame() {
        final long millis = ThreadLocalRandom.current().nextLong(
                TARGET_MIN.toMillis() / 10, TARGET_MAX.toMillis() / 10 + 1) * 10;
        this.targetTime = Duration.ofMillis(millis);
    }

    public BlindTimerGame(Duration targetTime) {
        this.targetTime = targetTime;
    }

    @Override
    public void setUp(List<Gamer> gamers) {
        final List<Player> castList = gamers.stream()
                .map(g -> Player.createGuest(new PlayerName(g.name()), g.userId()))
                .toList();
        this.players = new BlindTimerPlayers(castList);
    }

    @Override
    public MiniGameResult getResult() {
        return MiniGameResult.fromAscending(getScores());
    }

    @Override
    public Map<Gamer, MiniGameScore> getScores() {
        return players.stream()
                .collect(Collectors.toMap(
                        p -> p.getPlayer().toGamer(),
                        this::calculateScore
                ));
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.BLIND_TIMER;
    }

    public boolean stop(PlayerName playerName, Instant now) {
        validatePlaying();
        final BlindTimerPlayer player = players.findByName(playerName);
        final Duration elapsed = Duration.between(startTime, now);
        return player.stop(elapsed);
    }

    public void markAllTimedOut() {
        players.stream()
                .filter(p -> !p.isStopped())
                .forEach(BlindTimerPlayer::markTimedOut);
    }

    public boolean isAllStopped() {
        return players.isAllStopped();
    }

    public void startPlaying() {
        this.startTime = Instant.now();
        state.set(BlindTimerGameState.PLAYING);
    }

    public void updateState(BlindTimerGameState newState) {
        state.set(newState);
    }

    public BlindTimerGameState getState() {
        return state.get();
    }

    public boolean isPlaying() {
        return state.get() == BlindTimerGameState.PLAYING;
    }

    public boolean tryFinish() {
        return state.compareAndSet(BlindTimerGameState.PLAYING, BlindTimerGameState.DONE);
    }

    public void cancelTimeout() {
        if (timeoutFuture != null && !timeoutFuture.isDone()) {
            timeoutFuture.cancel(false);
        }
    }

    public BlindTimerPlayer findPlayer(PlayerName name) {
        return players.findByName(name);
    }

    private MiniGameScore calculateScore(BlindTimerPlayer player) {
        if (player.isTimedOut()) {
            return BlindTimerScore.ofTimeout();
        }
        return BlindTimerScore.ofNormal(targetTime, player.getStoppedElapsed());
    }

    private void validatePlaying() {
        if (state.get() != BlindTimerGameState.PLAYING) {
            throw new BusinessException(
                    BlindTimerGameErrorCode.NOT_PLAYING_STATE,
                    "현재 게임 상태가 플레이 중이 아닙니다: " + state.get()
            );
        }
    }
}
