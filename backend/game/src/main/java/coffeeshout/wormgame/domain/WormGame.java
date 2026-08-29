package coffeeshout.wormgame.domain;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.Playable;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * 지렁이 게임 — Tron식 궤적 서바이벌. 좌표계 중심은 (0,0)이고 모든 판정·시간은 틱에서 유도된다.
 * 규칙·수치의 SSOT는 설계 문서 v0.3(이슈 #1681).
 */
@Getter
public class WormGame implements Playable {

    private final WormGameRules rules;

    private WormGameState state;
    private Worms worms;
    private int playerCount;
    private long tickCount;
    private boolean roundOver;

    public WormGame(WormGameRules rules) {
        this.rules = rules;
    }

    @Override
    public void setUp(List<Gamer> gamers) {
        this.playerCount = gamers.size();
        this.worms = Worms.spawn(gamers, rules.initialRadius(playerCount));
        this.state = WormGameState.DESCRIPTION;
        this.tickCount = 0;
        this.roundOver = false;
    }

    public void updateState(WormGameState state) {
        this.state = state;
    }

    public void steer(String playerName, double angle, long seq) {
        validatePlaying();
        worms.findByName(playerName).steer(angle, seq);
    }

    /**
     * 한 틱 진행. 판정은 틱 시작 스냅샷 기준 — 이동·충돌 검사가 끝난 뒤에야 새 머리가 궤적에
     * 붙고 사망이 일괄 적용되므로, 같은 틱 안의 판정 순서에 생사가 갈리지 않는다.
     */
    public void tick() {
        if (state != WormGameState.PLAYING || roundOver) {
            return;
        }
        tickCount++;
        final double radius = rules.arenaRadius(playerCount, tickCount);
        final List<Worm> aliveAtTickStart = worms.alive();

        for (final Worm worm : aliveAtTickStart) {
            worm.advance(rules.speedPerTick(tickCount), rules.omegaPerTick(tickCount));
        }

        final Set<Worm> deaths = new LinkedHashSet<>();
        final boolean invincible = tickCount <= rules.invincibleTicks();
        for (final Worm worm : aliveAtTickStart) {
            if (worm.distanceFromCenter() > radius) {
                deaths.add(worm); // 경계는 무적 중에도 즉사
                continue;
            }
            if (!invincible && hitsAnyTrail(worm, aliveAtTickStart)) {
                deaths.add(worm);
            }
        }
        if (!invincible) {
            collectHeadOnDeaths(aliveAtTickStart, deaths);
        }

        deaths.forEach(worm -> worm.die(tickCount));
        aliveAtTickStart.forEach(Worm::appendHeadToTrail);

        if (worms.aliveCount() <= 1) {
            roundOver = true;
        }
    }

    public boolean isPlaying() {
        return state == WormGameState.PLAYING;
    }

    public double currentRadius() {
        return rules.arenaRadius(playerCount, tickCount);
    }

    @Override
    public MiniGameResult getResult() {
        return MiniGameResult.fromDescending(getScores());
    }

    @Override
    public Map<Gamer, MiniGameScore> getScores() {
        return worms.all().stream().collect(Collectors.toMap(Worm::getGamer, this::convertScore));
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.WORM_GAME;
    }

    /** 틱 델타용 — 전원의 머리 상태(사망자 포함, 클라가 alive로 판별). */
    public List<WormPosition> positions() {
        return worms.all().stream().map(WormPosition::of).toList();
    }

    /** 스냅샷용 — 전원의 샘플링된 궤적. */
    public List<WormTrailSnapshot> trailSnapshots(int stride) {
        return worms.all().stream()
                .map(worm -> WormTrailSnapshot.of(worm, stride))
                .toList();
    }

    private MiniGameScore convertScore(Worm worm) {
        // 생존자는 마지막 사망자보다 한 틱 더 산 것으로 계산해 반드시 위 순위가 된다.
        final long survivalTicks = worm.isAlive() ? tickCount + 1 : worm.getDeathTick();
        return new WormGameScore(survivalTicks * rules.tickMillis());
    }

    private boolean hitsAnyTrail(Worm mover, List<Worm> aliveAtTickStart) {
        for (final Worm owner : aliveAtTickStart) {
            if (owner == mover) {
                continue; // 자기 궤적은 통과한다 — 다른 지렁이 게임과 같은 조작감(#1722).
            }
            // 타인 궤적은 최신 ~150ms("마르지 않은 페인트")를 판정에서 제외한다.
            final Trail trail = owner.getTrail();
            final int checkable = trail.segmentCount() - rules.wetPaintSkipSegments();
            for (int i = 0; i < checkable; i++) {
                final Point start = trail.start(i);
                final Point end = trail.end(i);
                final double distance = SegmentGeometry.distance(
                        mover.getPreviousX(),
                        mover.getPreviousY(),
                        mover.getX(),
                        mover.getY(),
                        start.x(),
                        start.y(),
                        end.x(),
                        end.y());
                if (distance < rules.trailRadius()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void collectHeadOnDeaths(List<Worm> aliveAtTickStart, Set<Worm> deaths) {
        for (int i = 0; i < aliveAtTickStart.size(); i++) {
            for (int j = i + 1; j < aliveAtTickStart.size(); j++) {
                final Worm first = aliveAtTickStart.get(i);
                final Worm second = aliveAtTickStart.get(j);
                final double distance = SegmentGeometry.distance(
                        first.getPreviousX(),
                        first.getPreviousY(),
                        first.getX(),
                        first.getY(),
                        second.getPreviousX(),
                        second.getPreviousY(),
                        second.getX(),
                        second.getY());
                if (distance < rules.trailRadius()) {
                    // 머리끼리 정면충돌은 둘 다 사망(동점) — 한쪽만 살리면 판정 순서 논란이 된다.
                    deaths.add(first);
                    deaths.add(second);
                }
            }
        }
    }

    private void validatePlaying() {
        if (state != WormGameState.PLAYING) {
            throw new BusinessException(WormGameErrorCode.NOT_PLAYING_STATE, "현재 상태: " + state);
        }
    }
}
