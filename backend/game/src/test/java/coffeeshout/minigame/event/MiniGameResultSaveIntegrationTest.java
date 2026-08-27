package coffeeshout.minigame.event;

import static org.awaitility.Awaitility.await;

import coffeeshout.GameModuleIntegrationTest;
import coffeeshout.blindtimer.domain.BlindTimerGame;
import coffeeshout.blockstacking.domain.BlockStackingGame;
import coffeeshout.fixture.CardGameDeckStub;
import coffeeshout.fixture.CardGameFake;
import coffeeshout.fixture.GamerFixture;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.Playable;
import coffeeshout.laddergame.domain.LadderGame;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.nunchi.config.NunchiTimingProperties;
import coffeeshout.nunchi.domain.NunchiGame;
import coffeeshout.room.application.port.RoomEntityRepository;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.room.domain.service.JoinCodeGenerator;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.settlement.infra.SettlementStreamKey;
import coffeeshout.speedtouch.application.SpeedTouchGameProgressHandler;
import coffeeshout.speedtouch.domain.SpeedTouchGame;
import coffeeshout.speedtouch.domain.SpeedTouchPlayer;
import coffeeshout.user.infra.persistence.UserEntity;
import coffeeshout.user.infra.persistence.UserJpaRepository;
import java.time.Duration;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 미니게임 종료 → 결과 저장 이음매를 게임별로 보장한다(#1663).
 *
 * <p>검증 대상은 개별 게임의 진행 규칙이 아니라 <b>종료 시 {@code MiniGameFinishedEvent}의 부수효과</b>(결과
 * 영속·시즌 정산 Outbox 기록)다. 이 이음매는 어느 계층에서도 검증되지 않아, 레이싱 결과가 프로덕션에서 통째로
 * 유실되는 동안에도 전체 스위트가 초록이었다(#1662). 서비스 테스트는 이벤트 퍼블리셔가 mock이라 리스너가 아예
 * 돌지 않고, 리스너 단위 테스트는 저장소를 mock한 채 {@code handle}을 직접 부르며, 게임별 IT는 종료 <b>신호</b>
 * (DONE 브로드캐스트)까지만 본다. DONE 브로드캐스트는 저장 이벤트보다 앞줄에 예약되므로 저장이 통째로 깨져도
 * 종료만 보는 테스트는 통과한다.
 *
 * <p>레이싱은 {@code RacingGameIntegrationTest}가 덮으므로 여기서는 나머지 6종을 다룬다. 게임별 IT에 나눠 넣지
 * 않은 이유는 기존 IT들이 방을 DB에 만들지 않고({@code :game}만으로 시작) 저장 경로가 애초에 돌지 않기 때문이다 —
 * 6곳에 같은 방 영속 셋업을 복붙하는 대신 여기 한 벌만 둔다.
 *
 * <p>WebSocket을 쓰지 않는다. 이 이음매는 STOMP 전송과 무관하고, 게임 IT들이 겪은 트레일링 브로드캐스트 누수를
 * 피할 수 있다.
 */
class MiniGameResultSaveIntegrationTest extends GameModuleIntegrationTest {

    @Autowired
    GameSessionService gameSessionService;

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    RoomEntityRepository roomEntityRepository;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    JoinCodeGenerator joinCodeGenerator;

    @Autowired
    NunchiTimingProperties nunchiTiming;

    @Autowired
    SpeedTouchGameProgressHandler speedTouchProgressHandler;

    @Autowired
    UserJpaRepository userJpaRepository;

    JoinCode joinCode;
    Gamer host;
    List<Gamer> gamers;

    @BeforeEach
    void setUp() {
        joinCode = joinCodeGenerator.generate();
        host = GamerFixture.호스트_꾹이();
        // Outbox 기록은 회원(userId != null)만 담기므로 전원 게스트면 정산 이벤트 자체가 안 만들어진다.
        // 호스트를 회원으로 만들면 세션 호스트(게스트로 생성) 동일성 비교가 얽히므로 루키를 회원으로 둔다.
        gamers = 루키를_회원으로_바꾼_명단(회원_한_명을_만든다());
    }

    @Test
    void 카드게임_종료시_결과와_정산_이벤트가_저장된다() {
        게임을_시작한다(new CardGameFake(new CardGameDeckStub()));

        결과_저장과_정산_아웃박스를_확인한다(MiniGameType.CARD_GAME);
    }

    @Test
    void 사다리게임_종료시_결과와_정산_이벤트가_저장된다() {
        게임을_시작한다(new LadderGame());

        결과_저장과_정산_아웃박스를_확인한다(MiniGameType.LADDER_GAME);
    }

    @Test
    void 블록쌓기_종료시_결과와_정산_이벤트가_저장된다() {
        게임을_시작한다(new BlockStackingGame());

        결과_저장과_정산_아웃박스를_확인한다(MiniGameType.BLOCK_STACKING);
    }

    @Test
    void 눈치게임_종료시_결과와_정산_이벤트가_저장된다() {
        // 아무도 누르지 않으면 idle 타임아웃(500ms)으로 종료된다.
        게임을_시작한다(new NunchiGame(nunchiTiming.numberWindow().toMillis()));

        결과_저장과_정산_아웃박스를_확인한다(MiniGameType.NUNCHI_GAME);
    }

    @Test
    void 블라인드타이머_종료시_결과와_정산_이벤트가_저장된다() {
        // 종료는 targetTime + timeoutBuffer(3s)에 예약된다. 목표 시간을 줄여 입력 없이 타임아웃으로 끝낸다
        // (미입력 플레이어는 BlindTimerScore.ofTimeout으로 채점되므로 결과는 그대로 만들어진다).
        게임을_시작한다(new BlindTimerGame(Duration.ofMillis(500)));

        결과_저장과_정산_아웃박스를_확인한다(MiniGameType.BLIND_TIMER);
    }

    @Test
    void 스피드터치_종료시_결과와_정산_이벤트가_저장된다() {
        final SpeedTouchGame game = new SpeedTouchGame();
        게임을_시작한다(game);

        전원_완주시킨다(game);

        결과_저장과_정산_아웃박스를_확인한다(MiniGameType.SPEED_TOUCH);
    }

    /**
     * 스피드터치만 종료에 입력이 필요하다 — playing 타임아웃이 30초라 기다릴 수 없다.
     *
     * <p>터치를 WS로 보내지 않고 컨슈머가 호출하는 애플리케이션 진입점을 직접 부른다. 전원 25회 = 100번의 WS
     * 왕복은 느린 데다, Rate Limiter가 세션당 초당 20건 초과분을 조용히 드롭해 완주가 부하에 따라 갈린다(#1664).
     * 종료 판정({@code isAllFinished} → {@code finishGame})은 이 진입점 안에서 일어나므로 저장 경로는 동일하다.
     */
    private void 전원_완주시킨다(SpeedTouchGame game) {
        await().atMost(Duration.ofSeconds(5)).until(game::isPlaying);

        for (Gamer gamer : gamers) {
            for (int number = 1; number <= SpeedTouchPlayer.LAST_NUMBER; number++) {
                speedTouchProgressHandler.handleTouch(joinCode.getValue(), gamer.getName(), number);
            }
        }
    }

    /**
     * 프로덕션 시작 경로({@code :room}의 {@code MiniGameStartConsumer}가 발행하는 {@code GameStartReadyEvent})부터
     * 태운다. 기존 게임 IT들처럼 {@code service.start()}를 직접 부르면 {@code MiniGamePersistenceService}가
     * 건너뛰어져 {@code MiniGameEntity}가 없고, 저장 리스너가 "미니게임 엔티티가 존재하지 않습니다"로 죽는다.
     *
     * <p>방은 도메인 {@code Room}(인메모리)과 {@code RoomEntity}(DB)를 둘 다 채워야 한다 — 저장 리스너가
     * {@code RoomSnapshotQuery}로 room_session id와 player id를 해석하기 때문이다.
     */
    private void 게임을_시작한다(Playable game) {
        final Room room = RoomFixture.호스트_꾹이(joinCode);
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        roomRepository.save(room);
        roomEntityRepository.save(new RoomEntity(joinCode.getValue()));

        gameSessionService.initSession(joinCode, host);
        gameSessionService.getSession(joinCode).replaceGames(host, List.of(game));

        eventPublisher.publishEvent(
                new GameStartReadyEvent("evt-" + joinCode.getValue(), joinCode.getValue(), host.getName(), gamers));
    }

    /**
     * 저장은 게임마다 다른 스케줄러 스레드에서 일어나므로 폴링으로 기다린다. 상한은 가장 느린 카드게임
     * (2라운드 × playing 2s)을 기준으로 넉넉히 잡는다.
     */
    private void 결과_저장과_정산_아웃박스를_확인한다(MiniGameType miniGameType) {
        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(저장된_결과_수(miniGameType)).isEqualTo(gamers.size());
                    softly.assertThat(정산_아웃박스_수(miniGameType)).isEqualTo(1);
                }));
    }

    private Integer 저장된_결과_수(MiniGameType miniGameType) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mini_game_result WHERE mini_game_type = ?", Integer.class, miniGameType.name());
    }

    /**
     * 정산 이벤트는 결과 저장과 같은 트랜잭션에서 Outbox에 기록된다(#1610). 릴레이는 행을 지우지 않고 상태만 바꾼다.
     *
     * <p>스트림 키만으로도 테스트당 게임이 하나라 충분하지만 payload의 게임 타입까지 건다. 게임 스케줄러는 컨텍스트
     * 수명이라 앞선 테스트가 남긴 종료 태스크가 이 테스트 도중 발화할 수 있고, 그때 격리를 보장하는 것이 타입
     * 필터다(레이싱 IT가 같은 이유로 스케줄러 큐를 비운다).
     */
    private Integer 정산_아웃박스_수(MiniGameType miniGameType) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE stream_key = ? AND payload LIKE ?",
                Integer.class,
                SettlementStreamKey.RESULT.getRedisKey(),
                "%" + miniGameType.name() + "%");
    }

    private Long 회원_한_명을_만든다() {
        return userJpaRepository.save(new UserEntity("T1663", "루키")).getId();
    }

    private List<Gamer> 루키를_회원으로_바꾼_명단(Long userId) {
        return GamerFixture.꾹이_루키_엠제이_한스().stream()
                .map(gamer ->
                        "루키".equals(gamer.getName()) ? Gamer.of(gamer.getName(), userId, gamer.getColorIndex()) : gamer)
                .toList();
    }
}
