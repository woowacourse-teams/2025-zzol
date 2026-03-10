package coffeeshout.speedtouch.domain;

import coffeeshout.global.exception.custom.InvalidStateException;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.Playable;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

@Getter
public class SpeedTouchGame implements Playable {

    private SpeedTouchPlayers players;
    private SpeedTouchGameState state;
    private Instant startTime;

    @Setter
    private ScheduledFuture<?> timeoutFuture;

    public SpeedTouchGame() {
        this.state = SpeedTouchGameState.DESCRIPTION;
    }

    @Override
    public void setUp(List<Player> playerList) {
        this.players = new SpeedTouchPlayers(playerList);
    }

    @Override
    public MiniGameResult getResult() {
        return MiniGameResult.fromAscending(getScores());
    }

    @Override
    public Map<Player, MiniGameScore> getScores() {
        return players.stream()
                .collect(Collectors.toMap(
                        SpeedTouchPlayer::getPlayer,
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
        this.state = SpeedTouchGameState.PLAYING;
        this.startTime = Instant.now();
    }

    public void updateState(SpeedTouchGameState state) {
        this.state = state;
    }

    public boolean isPlaying() {
        return state == SpeedTouchGameState.PLAYING;
    }

    public boolean isDone() {
        return state == SpeedTouchGameState.DONE;
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
        if (state != SpeedTouchGameState.PLAYING) {
            throw new InvalidStateException(
                    SpeedTouchGameErrorCode.NOT_PLAYING_STATE,
                    "현재 게임 상태가 플레이 중이 아닙니다: " + state
            );
        }
    }
}
