package coffeeshout.racinggame.domain;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.Playable;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
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
    // 주행 중 감속률 — 매 틱 이 비율로 줄어든다.
    //
    // 막는 것은 손을 떼는 게 아니라 '탭 메시지가 끊긴 채 이전 속도로 자동 주행하는 것'이다.
    // 손만 떼면 여기까지 오지 않는다 — 프론트는 안 눌러도 200ms마다 tapCount 0을 보내고,
    // TapPerSecondSpeedCalculator가 그걸 MIN_SPEED로 환산한다. 메시지 자체가 끊기면
    // (백그라운드 탭·화면 꺼짐·WS 끊김·변조 클라이언트) lastSpeedUpdateTime이 멈춰
    // 이전 speed가 굳으므로, 서버가 시간으로 깎지 않으면 조작 없이 최고 속도로 완주한다.
    //
    // 틱당 비율이라 틱 길이에 의존한다. racing-game.timing.move-interval(운영 100ms)을
    // 전제로 잡았으므로 주기를 바꾸면 이 값도 같이 봐야 한다.
    public static final double SPEED_DECAY_RATE = 0.9;
    public static final int FINISH_LINE = 3000;
    public static final int START_LINE = 0;

    public static final long MOVE_INTERVAL_MILLIS = 100L;

    private Instant startTime;
    private Runners runners;
    private RacingGameState state;

    @Setter
    private ScheduledFuture<?> autoMoveFuture;

    @Override
    public void setUp(List<Gamer> gamers) {
        this.runners = new Runners(gamers);
        this.state = RacingGameState.DESCRIPTION;
    }

    public void setUpStart() {
        this.runners.initialSpeed();
        this.startTime = Instant.now();
        this.runners.initialLastTapTime(startTime);
    }

    public void moveAll(Instant now) {
        runners.moveAll(now);
    }

    public boolean isStarted() {
        return state == RacingGameState.PLAYING;
    }

    public void stopAutoMove() {
        if (autoMoveFuture != null && !autoMoveFuture.isDone()) {
            autoMoveFuture.cancel(false);
        }
    }

    public void updateSpeed(String playerName, int tapCount, SpeedCalculator speedCalculator, Instant now) {
        validatePlaying();
        runners.updateSpeed(playerName, tapCount, speedCalculator, now);
    }

    private void validatePlaying() {
        if (state != RacingGameState.PLAYING) {
            throw new BusinessException(RacingGameErrorCode.NOT_PLAYING_STATE, "현재 게임 상태가 플레이 중이 아닙니다: " + state);
        }
    }

    @Override
    public MiniGameResult getResult() {
        return MiniGameResult.fromAscending(getScores());
    }

    @Override
    public Map<Gamer, MiniGameScore> getScores() {
        return runners.stream().collect(Collectors.toMap(Runner::getGamer, this::convertScore));
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
