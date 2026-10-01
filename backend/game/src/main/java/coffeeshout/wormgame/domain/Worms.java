package coffeeshout.wormgame.domain;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.global.exception.custom.BusinessException;
import java.util.List;

public class Worms {

    /** 핀휠 스폰 — 중심 지향 그대로면 무적 종료 순간 전원이 중심에 수렴해 대량 사망을 강제한다(교차 리뷰). */
    static final double PINWHEEL_OFFSET_RADIANS = Math.PI / 4;

    private static final double SPAWN_DISTANCE_RATIO = 0.82;

    private final List<Worm> worms;

    private Worms(List<Worm> worms) {
        this.worms = worms;
    }

    static Worms spawn(List<Gamer> gamers, double arenaRadius) {
        final List<Worm> spawned = gamers.stream().map(Worm::new).toList();
        final int count = spawned.size();
        for (int i = 0; i < count; i++) {
            final double slotAngle = 2 * Math.PI * i / count - Math.PI / 2;
            final double distance = arenaRadius * SPAWN_DISTANCE_RATIO;
            spawned.get(i)
                    .spawnAt(
                            Math.cos(slotAngle) * distance,
                            Math.sin(slotAngle) * distance,
                            slotAngle + Math.PI + PINWHEEL_OFFSET_RADIANS);
        }
        return new Worms(spawned);
    }

    Worm findByName(String playerName) {
        return worms.stream()
                .filter(worm -> worm.getGamer().getName().equals(playerName))
                .findFirst()
                .orElseThrow(() -> new BusinessException(WormGameErrorCode.NOT_EXIST_WORM, playerName));
    }

    List<Worm> alive() {
        return worms.stream().filter(Worm::isAlive).toList();
    }

    long aliveCount() {
        return worms.stream().filter(Worm::isAlive).count();
    }

    public List<Worm> all() {
        return List.copyOf(worms);
    }
}
