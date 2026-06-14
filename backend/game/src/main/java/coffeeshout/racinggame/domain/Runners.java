package coffeeshout.racinggame.domain;

import coffeeshout.gamecommon.Gamer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;

@Getter
public class Runners {

    private final List<Runner> runners;

    public Runners(List<Gamer> gamers) {
        this.runners = Collections.synchronizedList(new ArrayList<>());
        gamers.forEach(gamer -> runners.add(new Runner(gamer)));
    }

    public void updateSpeed(String playerName, int tapCount, SpeedCalculator speedCalculator, Instant now) {
        final Runner runner = findRunnerByName(playerName);
        runner.updateSpeed(tapCount, speedCalculator, now);
    }

    public void moveAll(Instant now) {
        runners.forEach(runner -> runner.move(now));
    }

    public Optional<Runner> findWinner() {
        return runners.stream()
                .filter(Runner::isFinished)
                .min(Comparator.comparing(Runner::getFinishTime));
    }

    public boolean hasWinner() {
        return findWinner().isPresent();
    }

    public Map<Runner, Integer> getPositions() {
        final Map<Runner, Integer> positions = new LinkedHashMap<>();
        runners.forEach(runner -> positions.put(runner, runner.getPosition()));
        return positions;
    }

    public Map<Runner, Integer> getSpeeds() {
        final Map<Runner, Integer> speeds = new LinkedHashMap<>();
        runners.forEach(runner -> speeds.put(runner, runner.getSpeed()));
        return speeds;
    }

    public boolean isAllFinished() {
        return runners.stream().allMatch(Runner::isFinished);
    }

    public void initialSpeed() {
        runners.forEach(Runner::initializeSpeed);
    }

    public void initialLastTapTime(Instant time) {
        runners.forEach(runner -> runner.initializeLastSpeedUpdateTime(time));
    }

    public Stream<Runner> stream() {
        return runners.stream();
    }

    private Runner findRunnerByName(String playerName) {
        return runners.stream()
                .filter(runner -> runner.getGamer().getName().equals(playerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 플레이어의 러너를 찾을 수 없습니다."));
    }
}
