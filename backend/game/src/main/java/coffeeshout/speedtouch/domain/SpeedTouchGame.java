package coffeeshout.speedtouch.domain;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.Playable;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

@Getter
public class SpeedTouchGame implements Playable {

    private SpeedTouchPlayers players;
    private final AtomicReference<SpeedTouchGameState> state =
            new AtomicReference<>(SpeedTouchGameState.DESCRIPTION);
    private volatile Instant startTime;

    @Setter
    private ScheduledFuture<?> timeoutFuture;

    @Override
    public void setUp(List<Gamer> gamers) {
        final List<Player> castList = gamers.stream()
                .map(g -> Player.createGuest(new PlayerName(g.name()), g.userId()))
                .toList();
        this.players = new SpeedTouchPlayers(castList);
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
        return MiniGameType.SPEED_TOUCH;
    }

    public boolean touch(PlayerName playerName, int number, Instant now) {
        validatePlaying();
        final SpeedTouchPlayer player = players.findByName(playerName);
        return player.touch(number, now);
    }

    public boolean isAllFinished() {
        return players.isAllFinished();
    }

    public void startPlaying() {
        this.startTime = Instant.now();
        state.set(SpeedTouchGameState.PLAYING);
    }

    public void updateState(SpeedTouchGameState newState) {
        state.set(newState);
    }

    public SpeedTouchGameState getState() {
        return state.get();
    }

    public boolean isPlaying() {
        return state.get() == SpeedTouchGameState.PLAYING;
    }

    public boolean isDone() {
        return state.get() == SpeedTouchGameState.DONE;
    }

    public boolean tryFinish() {
        return state.compareAndSet(SpeedTouchGameState.PLAYING, SpeedTouchGameState.DONE);
    }

    public void cancelTimeout() {
        if (timeoutFuture != null && !timeoutFuture.isDone()) {
            timeoutFuture.cancel(false);
        }
    }

    public SpeedTouchPlayer findPlayer(PlayerName name) {
        return players.findByName(name);
    }

    private MiniGameScore calculateScore(SpeedTouchPlayer player) {
        if (player.isFinished()) {
            return SpeedTouchScore.ofFinished(player.calculateFinishMillis(startTime));
        }
        return SpeedTouchScore.ofDnf(player.getProgress());
    }

    private void validatePlaying() {
        if (state.get() != SpeedTouchGameState.PLAYING) {
            throw new BusinessException(
                    SpeedTouchGameErrorCode.NOT_PLAYING_STATE,
                    "현재 게임 상태가 플레이 중이 아닙니다: " + state.get()
            );
        }
    }
}
