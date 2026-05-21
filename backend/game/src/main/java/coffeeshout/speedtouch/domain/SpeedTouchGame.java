package coffeeshout.speedtouch.domain;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.minigame.domain.Gamer;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.domain.Playable;
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
    private List<Gamer> gamers;
    private final AtomicReference<SpeedTouchGameState> state =
            new AtomicReference<>(SpeedTouchGameState.DESCRIPTION);
    private volatile Instant startTime;

    @Setter
    private ScheduledFuture<?> timeoutFuture;

    @Override
    public void setUp(List<Gamer> gamers) {
        this.gamers = List.copyOf(gamers);
        this.players = new SpeedTouchPlayers(gamers.stream().map(Gamer::name).toList());
    }

    @Override
    public List<Gamer> getGamers() {
        return gamers;
    }

    @Override
    public MiniGameResult getResult() {
        return MiniGameResult.fromAscending(getScores());
    }

    @Override
    public Map<Gamer, MiniGameScore> getScores() {
        final Map<PlayerName, Gamer> nameToGamer = gamers.stream()
                .collect(Collectors.toMap(Gamer::name, g -> g));
        return players.stream()
                .collect(Collectors.toMap(
                        p -> nameToGamer.get(p.getPlayerName()),
                        this::calculateScore
                ));
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.SPEED_TOUCH;
    }

    public boolean touch(Gamer gamer, int number, Instant now) {
        validatePlaying();
        gamer.validateAgainst(this.gamers);
        final SpeedTouchPlayer player = players.findByName(gamer.name());
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
