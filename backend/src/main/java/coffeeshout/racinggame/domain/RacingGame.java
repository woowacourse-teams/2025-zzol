package coffeeshout.racinggame.domain;

import coffeeshout.global.exception.custom.InvalidStateException;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.Playable;
import coffeeshout.room.domain.player.Player;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

@Getter
public class RacingGame implements Playable {

    public static final int INITIAL_SPEED = 0;
    public static final int MIN_SPEED = 3;
    public static final int MAX_SPEED = 60;
    public static final int CLICK_PER_SPEED_SCALE = 1;
    public static final int FINISH_LINE = 3000;
    public static final int START_LINE = 0;

    public static final long MOVE_INTERVAL_MILLIS = 100L;

    private Instant startTime;
    private Runners runners;
    private RacingGameState state;

    @Setter
    private ScheduledFuture<?> autoMoveFuture;

    @Override
    public void setUp(List<Player> players) {
        this.runners = new Runners(players);
        this.state = RacingGameState.DESCRIPTION;
    }

    public void setUpStart() {
        this.runners.initialSpeed();
        this.startTime = Instant.now();
        this.runners.initialLastTapTime(startTime);
    }

    public void moveAll() {
        runners.moveAll(Instant.now());
    }

    public boolean isStarted() {
        return state == RacingGameState.PLAYING;
    }

    public void stopAutoMove() {
        if (autoMoveFuture != null && !autoMoveFuture.isDone()) {
            autoMoveFuture.cancel(true);
        }
    }

    public void updateSpeed(Player player, int tapCount, SpeedCalculator speedCalculator, Instant now) {
        validatePlaying();
        runners.updateSpeed(player, tapCount, speedCalculator, now);
    }

    private void validatePlaying() {
        if (state != RacingGameState.PLAYING) {
            throw new InvalidStateException(
                    RacingGameErrorCode.NOT_PLAYING_STATE,
                    "현재 게임 상태가 플레이 중이 아닙니다: " + state
            );
        }
    }

    @Override
    public MiniGameResult getResult() {
        return MiniGameResult.fromAscending(getScores());
    }

    @Override
    public Map<Player, MiniGameScore> getScores() {
        return runners.stream().collect(Collectors.toMap(Runner::getPlayer, this::convertScore));
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.RACING_GAME;
    }

    public Map<Runner, Integer> getPositions() {
        return runners.getPositions();
    }

    public boolean isDone() {
        return state == RacingGameState.DONE;
    }

    public boolean isFinished() {
        return runners.isAllFinished();
    }

    public boolean isAllStopped() {
        return runners.stream().allMatch(Runner::isStopped);
    }

    public void updateState(RacingGameState state) {
        this.state = state;
    }

    private MiniGameScore convertScore(Runner runner) {
        final Instant finishTime = runner.getFinishTime();
        final long durationMillis = diffInstant(getStartTime(), finishTime);
        return new RacingGameScore(durationMillis);
    }

    private long diffInstant(Instant startTime, Instant endTime) {
        return Duration.between(startTime, endTime).toMillis();
    }
}
