package coffeeshout.nunchi.domain;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.Playable;
import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 눈치게임 도메인 상태기계(ADR-0031). 순수 도메인이라 시간을 직접 들지 않고 권위 시각({@link Instant})을
 * 주입받아 판정만 한다. 윈도우(300ms)·쿨다운·idle 타이머의 <b>구동</b>은 Flow가 맡고
 * ({@link #closeWindow()}·{@link #endCooldown()}·{@link #finishByTimeout()} 호출), 윈도우
 * <b>판정 규칙</b>(주입된 {@code windowMillis})만 여기 둔다(BlindTimer의 시각 주입 패턴).
 *
 * <p>핵심 규칙:
 * <ul>
 *   <li>각 번호는 첫 press가 anchor가 되어 즉시 STOOD(낙관적). anchor+{@code windowMillis} 안에 또
 *       눌리면 그 그룹 전원이 충돌해 OUT되고 카운터는 그 번호로 reset된다(N2/N3).</li>
 *   <li>충돌자는 1인 1press라 영구 OUT, 재입력은 무시. 비운 번호는 아직 안 누른 사람이 차지한다.</li>
 *   <li>점수는 {@link NunchiScore} 단일 long 밴드(정상=누른 시각, 충돌=그룹 anchor 공유, 미입력=최악).</li>
 * </ul>
 */
public class NunchiGame implements Playable {

    private final long windowMillis;
    private final List<Gamer> gamers = new ArrayList<>();
    private final Map<Gamer, NunchiScore> results = new LinkedHashMap<>();

    private NunchiState state;
    private int currentNumber;
    private PendingPress pending;        // nullable — 현재 번호의 첫 press(solo 확정 전)
    private OpenCollision openCollision; // nullable — COOLDOWN 동안 열린 충돌 그룹

    public NunchiGame(long windowMillis) {
        this.windowMillis = windowMillis;
    }

    @Override
    public void setUp(List<Gamer> gamers) {
        this.gamers.clear();
        this.gamers.addAll(gamers);
        this.results.clear();
        this.pending = null;
        this.openCollision = null;
        this.state = NunchiState.DESCRIPTION;
        this.currentNumber = 1;
    }

    /** 규칙 설명이 끝났을 때 Flow가 호출한다. {@code DESCRIPTION → PLAYING}으로 전이해 입력을 받기 시작한다. */
    public void startPlaying() {
        if (state == NunchiState.DESCRIPTION) {
            state = NunchiState.PLAYING;
        }
    }

    /**
     * 권위 시각 {@code at}으로 press를 판정한다. 잘못된 입력은 예외가 아니라
     * {@link PressOutcome#IGNORED}로 돌려준다(ADR 결정 1 — warn 로그만).
     */
    public PressResult press(Gamer gamer, Instant at) {
        if (state == NunchiState.DONE || results.containsKey(gamer)) {
            return PressResult.ignored(); // 종료됐거나 이미 일어섬/충돌해 OUT
        }
        if (state == NunchiState.DESCRIPTION) {
            return PressResult.ignored(); // 규칙 설명 중 — 아직 입력을 받지 않음
        }
        if (state == NunchiState.COLLISION_COOLDOWN) {
            return pressDuringCooldown(gamer, at);
        }
        if (pending != null && pending.gamer().equals(gamer)) {
            return PressResult.ignored(); // 같은 사람의 중복 press
        }
        if (pending == null) {
            pending = new PendingPress(gamer, at);
            return PressResult.stood(currentNumber);
        }
        if (withinWindow(pending.at(), at)) {
            return collide(gamer);
        }
        // 윈도우 경과: 직전 pending을 solo로 확정하고 이 press가 새 번호의 첫 press가 된다
        confirmSolo(pending);
        pending = new PendingPress(gamer, at);
        return PressResult.stood(currentNumber);
    }

    private PressResult pressDuringCooldown(Gamer gamer, Instant at) {
        if (withinWindow(openCollision.anchor(), at)) {
            results.put(gamer, NunchiScore.collision(openCollision.anchor().toEpochMilli()));
            openCollision.members().add(gamer);
            return PressResult.collided(currentNumber, openCollision.members());
        }
        return PressResult.ignored();
    }

    private PressResult collide(Gamer gamer) {
        final Instant anchor = pending.at();
        final long anchorMillis = anchor.toEpochMilli();
        final List<Gamer> members = new ArrayList<>(List.of(pending.gamer(), gamer));
        results.put(pending.gamer(), NunchiScore.collision(anchorMillis));
        results.put(gamer, NunchiScore.collision(anchorMillis));
        openCollision = new OpenCollision(members, anchor);
        pending = null;
        state = NunchiState.COLLISION_COOLDOWN;
        return PressResult.collided(currentNumber, members);
    }

    /** 윈도우가 입력 없이 닫혔을 때 Flow가 호출한다. pending이 살아남으면 solo로 확정한다. */
    public Optional<Gamer> closeWindow() {
        if (pending == null) {
            return Optional.empty();
        }
        final Gamer confirmed = pending.gamer();
        confirmSolo(pending);
        pending = null;
        return Optional.of(confirmed);
    }

    /** 충돌 쿨다운이 끝났을 때 Flow가 호출한다. 같은 번호로 PLAYING 재개. */
    public void endCooldown() {
        if (state == NunchiState.COLLISION_COOLDOWN) {
            state = NunchiState.PLAYING;
            openCollision = null;
        }
    }

    /** idle/하드캡 타임아웃 시 Flow가 호출한다. pending은 solo로 확정하고 남은 미입력자를 MISS로 종료한다. */
    public List<Gamer> finishByTimeout() {
        if (pending != null) {
            confirmSolo(pending);
            pending = null;
        }
        final List<Gamer> missed = new ArrayList<>();
        for (Gamer gamer : gamers) {
            if (!results.containsKey(gamer)) {
                results.put(gamer, NunchiScore.miss());
                missed.add(gamer);
            }
        }
        state = NunchiState.DONE;
        return missed;
    }

    /** 전원이 (정상/충돌로) 입력을 마쳤는가 — 조기 종료 트리거(ADR 결정 5). pending도 입력으로 친다. */
    public boolean isAllPressed() {
        return gamers.stream()
                .allMatch(g -> results.containsKey(g) || (pending != null && pending.gamer().equals(g)));
    }

    public boolean isFinished() {
        return state == NunchiState.DONE;
    }

    /**
     * 닉네임으로 참가자를 찾는다(방내 닉네임 유니크 불변식 전제 — ADR-0031 N6). 컨슈머가 받은
     * 닉네임 문자열을 {@code press}에 넘길 {@link Gamer}로 해석하기 위함이다. {@code Gamer} 동일성은
     * {@code (name, userId)}이므로 닉네임만으로 새 {@code Gamer}를 만들면 점수 맵 키와 어긋나, 반드시
     * {@code setUp}으로 주입된 원본 인스턴스를 돌려준다.
     */
    public Gamer findByName(String name) {
        return gamers.stream()
                .filter(gamer -> gamer.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        GlobalErrorCode.NOT_EXIST, "플레이어를 찾을 수 없습니다: " + name));
    }

    public NunchiState getState() {
        return state;
    }

    public int getCurrentNumber() {
        return currentNumber;
    }

    private boolean withinWindow(Instant anchor, Instant at) {
        return Duration.between(anchor, at).toMillis() <= windowMillis;
    }

    private void confirmSolo(PendingPress press) {
        results.put(press.gamer(), NunchiScore.solo(press.at().toEpochMilli()));
        currentNumber++;
    }

    @Override
    public MiniGameResult getResult() {
        return MiniGameResult.fromAscending(getScores());
    }

    @Override
    public Map<Gamer, MiniGameScore> getScores() {
        return new LinkedHashMap<>(results);
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.NUNCHI_GAME;
    }

    private record PendingPress(Gamer gamer, Instant at) {
    }

    private record OpenCollision(List<Gamer> members, Instant anchor) {
    }
}
