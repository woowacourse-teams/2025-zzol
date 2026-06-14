package coffeeshout.minigame.application;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import coffeeshout.cardgame.domain.CardGameScore;
import coffeeshout.fixture.StubPlayable;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.MiniGameFactory;
import coffeeshout.gamecommon.Playable;
import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.minigame.domain.GameSessionErrorCode;
import coffeeshout.minigame.domain.GameSessionRepository;
import coffeeshout.minigame.domain.GameSessionStatus;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.infra.MemoryGameSessionRepository;
import coffeeshout.minigame.event.dto.MiniGameSelectEvent;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GameSessionServiceTest {

    private static final JoinCode JOIN_CODE = new JoinCode("ABCD");
    private static final Gamer HOST = Gamer.guest("호스트");
    private static final Gamer GUEST = Gamer.guest("게스트");

    private GameSessionRepository repository;
    private GameSessionService service;

    @BeforeEach
    void setUp() {
        repository = new MemoryGameSessionRepository();
        service = new GameSessionService(repository, List.of());
    }

    private Playable game(MiniGameType type) {
        return new StubPlayable(type);
    }

    @Nested
    @DisplayName("세션 초기화(initSession)")
    class InitSession {

        @Test
        @DisplayName("세션이 없으면 새로 생성한다")
        void 세션이_없으면_새로_생성한다() {
            service.initSession(JOIN_CODE, HOST);

            assertThat(repository.existsByJoinCode(JOIN_CODE)).isTrue();
        }

        @Test
        @DisplayName("이미 존재하면 무시한다(멱등) — 기존 호스트를 유지한다")
        void 이미_존재하면_무시한다() {
            service.initSession(JOIN_CODE, HOST);
            service.initSession(JOIN_CODE, GUEST);

            assertThat(service.getSession(JOIN_CODE).getHost()).isEqualTo(HOST);
        }
    }

    @Nested
    @DisplayName("세션 조회(getSession / findSession)")
    class GetSession {

        @Test
        @DisplayName("존재하지 않는 세션을 getSession하면 NOT_EXIST 예외가 발생한다")
        void 존재하지_않는_세션을_조회하면_NOT_EXIST_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> service.getSession(JOIN_CODE),
                    GlobalErrorCode.NOT_EXIST);
        }

        @Test
        @DisplayName("존재하지 않는 세션을 findSession하면 빈 Optional을 반환한다")
        void 존재하지_않는_세션을_findSession하면_빈_Optional을_반환한다() {
            assertThat(service.findSession(JOIN_CODE)).isEmpty();
        }
    }

    @Nested
    @DisplayName("게임 시작/종료 위임(startGame / finishGame)")
    class StartAndFinish {

        @BeforeEach
        void initWithGames() {
            service.initSession(JOIN_CODE, HOST);
            // 서비스는 게임 선택(replaceGames)을 노출하지 않으므로 도메인으로 직접 대기열을 구성한다(Step 4에서 배선).
            final GameSession session = service.getSession(JOIN_CODE);
            session.replaceGames(HOST, List.of(game(MiniGameType.CARD_GAME), game(MiniGameType.RACING_GAME)));
        }

        @Test
        @DisplayName("startGame은 세션의 다음 게임을 시작하고 PLAYING으로 전이한다")
        void startGame은_다음_게임을_시작한다() {
            final Playable started = service.startGame(JOIN_CODE, HOST, List.of(HOST, GUEST));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(started.getMiniGameType()).isEqualTo(MiniGameType.CARD_GAME);
                softly.assertThat(service.getSession(JOIN_CODE).getStatus()).isEqualTo(GameSessionStatus.PLAYING);
            });
        }

        @Test
        @DisplayName("finishGame은 게임을 종료하고 선택 게임 총수(roundCount)를 반환한다")
        void finishGame은_라운드_수를_반환한다() {
            service.startGame(JOIN_CODE, HOST, List.of(HOST, GUEST));

            final int roundCount = service.finishGame(JOIN_CODE);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(roundCount).isEqualTo(2);
                softly.assertThat(service.getSession(JOIN_CODE).getStatus()).isEqualTo(GameSessionStatus.READY);
            });
        }
    }

    @Nested
    @DisplayName("게임 선택 교체(updateGames)")
    class UpdateGames {

        private MiniGameFactory factory(MiniGameType type) {
            return new MiniGameFactory() {
                @Override
                public MiniGameType type() {
                    return type;
                }

                @Override
                public Playable create(String joinCode) {
                    return new StubPlayable(type);
                }
            };
        }

        private GameSessionService serviceWithFactories(MiniGameType... types) {
            final List<MiniGameFactory> factories = new java.util.ArrayList<>();
            for (MiniGameType type : types) {
                factories.add(factory(type));
            }
            return new GameSessionService(repository, factories);
        }

        @Test
        @DisplayName("세션이 없으면 NOT_EXIST 예외가 발생한다(지연 생성 없음 — Option B)")
        void 세션이_없으면_예외가_발생한다() {
            // 세션은 방 생성 시 GameSessionInitConsumer가 권위 있는 호스트로 사전 생성한다.
            // 지연 생성을 제거했으므로 select가 init보다 먼저 도달하면 거짓 호스트를 신뢰하지 않고 거부한다.
            final GameSessionService sut = serviceWithFactories(MiniGameType.CARD_GAME, MiniGameType.RACING_GAME);
            final MiniGameSelectEvent event = new MiniGameSelectEvent(
                    JOIN_CODE.getValue(),
                    HOST.getName(),
                    List.of(MiniGameType.CARD_GAME, MiniGameType.RACING_GAME));

            assertCoffeeShoutException(() -> sut.updateGames(event), GlobalErrorCode.NOT_EXIST);
            assertThat(repository.existsByJoinCode(JOIN_CODE)).isFalse();
        }

        @Test
        @DisplayName("기존 세션이 있으면 선택 게임 목록을 통째로 교체한다")
        void 기존_세션이_있으면_교체한다() {
            final GameSessionService sut = serviceWithFactories(
                    MiniGameType.CARD_GAME, MiniGameType.RACING_GAME, MiniGameType.LADDER_GAME);
            sut.initSession(JOIN_CODE, HOST);
            sut.updateGames(new MiniGameSelectEvent(
                    JOIN_CODE.getValue(), HOST.getName(), List.of(MiniGameType.CARD_GAME)));

            sut.updateGames(new MiniGameSelectEvent(
                    JOIN_CODE.getValue(), HOST.getName(),
                    List.of(MiniGameType.RACING_GAME, MiniGameType.LADDER_GAME)));

            assertThat(sut.getSession(JOIN_CODE).getSelectedTypes())
                    .containsExactly(MiniGameType.RACING_GAME, MiniGameType.LADDER_GAME);
        }

        @Test
        @DisplayName("팩토리 맵으로 각 타입의 Playable을 생성한다")
        void 팩토리_맵으로_Playable을_생성한다() {
            final GameSessionService sut = serviceWithFactories(MiniGameType.CARD_GAME);
            sut.initSession(JOIN_CODE, HOST);
            final MiniGameSelectEvent event = new MiniGameSelectEvent(
                    JOIN_CODE.getValue(), HOST.getName(), List.of(MiniGameType.CARD_GAME));

            sut.updateGames(event);

            assertThat(sut.getSession(JOIN_CODE).getSelectedTypes())
                    .containsExactly(MiniGameType.CARD_GAME);
        }
    }

    @Nested
    @DisplayName("게임 조회(getScores / getRanks / getSelectedTypes)")
    class Queries {

        private void initSessionWithStartedGame() {
            service.initSession(JOIN_CODE, HOST);
            final GameSession session = service.getSession(JOIN_CODE);
            session.replaceGames(HOST, List.of(game(MiniGameType.CARD_GAME), game(MiniGameType.RACING_GAME)));
            service.startGame(JOIN_CODE, HOST, List.of(HOST, GUEST));
        }

        private void initSessionWithScoredGame(Map<Gamer, MiniGameScore> scores) {
            service.initSession(JOIN_CODE, HOST);
            service.getSession(JOIN_CODE)
                    .replaceGames(HOST, List.of(new StubPlayable(MiniGameType.CARD_GAME, scores)));
            service.startGame(JOIN_CODE, HOST, List.of(HOST, GUEST));
        }

        @Test
        @DisplayName("getSelectedTypes는 세션이 없으면 빈 목록을 반환한다")
        void getSelectedTypes는_세션이_없으면_빈_목록을_반환한다() {
            assertThat(service.getSelectedTypes(JOIN_CODE)).isEmpty();
        }

        @Test
        @DisplayName("getSelectedTypes는 대기 중인 게임 타입 목록을 반환한다")
        void getSelectedTypes는_대기_게임_타입을_반환한다() {
            initSessionWithStartedGame();

            assertThat(service.getSelectedTypes(JOIN_CODE)).containsExactly(MiniGameType.RACING_GAME);
        }

        @Test
        @DisplayName("getScores는 시작된 게임의 점수를 반환한다")
        void getScores는_시작된_게임의_점수를_반환한다() {
            initSessionWithStartedGame();

            assertThat(service.getScores(JOIN_CODE, MiniGameType.CARD_GAME)).isEmpty();
        }

        @Test
        @DisplayName("getScores는 세션이 없으면 NOT_EXIST 예외가 발생한다")
        void getScores는_세션이_없으면_NOT_EXIST_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> service.getScores(JOIN_CODE, MiniGameType.CARD_GAME),
                    GlobalErrorCode.NOT_EXIST);
        }

        @Test
        @DisplayName("getScores는 시작되지 않은 타입이면 GAME_NOT_FOUND 예외가 발생한다")
        void getScores는_시작되지_않은_타입이면_GAME_NOT_FOUND_예외가_발생한다() {
            initSessionWithStartedGame();

            assertCoffeeShoutException(
                    () -> service.getScores(JOIN_CODE, MiniGameType.LADDER_GAME),
                    GameSessionErrorCode.GAME_NOT_FOUND);
        }

        @Test
        @DisplayName("getScores는 게임이 계산한 점수 맵을 그대로 반환한다")
        void getScores는_게임의_점수_맵을_그대로_반환한다() {
            initSessionWithScoredGame(Map.of(
                    HOST, new CardGameScore(20),
                    GUEST, new CardGameScore(-10)));

            final Map<Gamer, MiniGameScore> result = service.getScores(JOIN_CODE, MiniGameType.CARD_GAME);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.get(HOST).getValue()).isEqualTo(20);
                softly.assertThat(result.get(GUEST).getValue()).isEqualTo(-10);
            });
        }

        @Test
        @DisplayName("getRanks는 시작된 게임의 결과를 반환한다")
        void getRanks는_시작된_게임의_결과를_반환한다() {
            initSessionWithStartedGame();

            assertThat(service.getRanks(JOIN_CODE, MiniGameType.CARD_GAME)).isNotNull();
        }

        @Test
        @DisplayName("getRanks는 점수 내림차순으로 순위를 매겨 반환한다")
        void getRanks는_점수_내림차순으로_순위를_반환한다() {
            initSessionWithScoredGame(Map.of(
                    HOST, new CardGameScore(20),
                    GUEST, new CardGameScore(-10)));

            final MiniGameResult result = service.getRanks(JOIN_CODE, MiniGameType.CARD_GAME);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getPlayerRank(HOST)).isEqualTo(1);
                softly.assertThat(result.getPlayerRank(GUEST)).isEqualTo(2);
            });
        }
    }

    @Nested
    @DisplayName("호스트 갱신(updateHost)")
    class UpdateHost {

        @Test
        @DisplayName("세션이 있으면 host를 갱신해 저장한다")
        void 세션이_있으면_host를_갱신한다() {
            service.initSession(JOIN_CODE, HOST);
            final Gamer newHost = Gamer.guest("새호스트");

            service.updateHost(JOIN_CODE, newHost);

            assertThat(service.getSession(JOIN_CODE).getHost()).isEqualTo(newHost);
        }

        @Test
        @DisplayName("세션이 없으면 예외 없이 조용히 건너뛴다(생명주기 멱등)")
        void 세션이_없으면_조용히_건너뛴다() {
            // 세션을 생성하지 않은 상태 — init/cleanup과 동일한 비throw 정책
            assertThatCode(() -> service.updateHost(JOIN_CODE, Gamer.guest("새호스트")))
                    .doesNotThrowAnyException();
            assertThat(repository.existsByJoinCode(JOIN_CODE)).isFalse();
        }
    }

    @Nested
    @DisplayName("세션 정리(deleteSession)")
    class DeleteSession {

        @Test
        @DisplayName("세션을 삭제하면 저장소에서 제거된다")
        void 세션을_삭제하면_저장소에서_제거된다() {
            service.initSession(JOIN_CODE, HOST);

            service.deleteSession(JOIN_CODE);

            assertThat(repository.existsByJoinCode(JOIN_CODE)).isFalse();
        }
    }
}
