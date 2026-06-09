package coffeeshout.minigame.application;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.StubPlayable;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.MiniGameFactory;
import coffeeshout.gamecommon.Playable;
import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.minigame.domain.GameSessionRepository;
import coffeeshout.minigame.domain.GameSessionStatus;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.infra.MemoryGameSessionRepository;
import coffeeshout.room.domain.event.MiniGameSelectEvent;
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
        service = new GameSessionService(repository, Map.of());
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
            final Map<MiniGameType, MiniGameFactory> factoryMap = new java.util.EnumMap<>(MiniGameType.class);
            for (MiniGameType type : types) {
                factoryMap.put(type, factory(type));
            }
            return new GameSessionService(repository, factoryMap);
        }

        @Test
        @DisplayName("세션이 없으면 호스트 이름으로 지연 생성하고 선택 게임으로 교체한다")
        void 세션이_없으면_지연_생성하고_교체한다() {
            final GameSessionService sut = serviceWithFactories(MiniGameType.CARD_GAME, MiniGameType.RACING_GAME);
            final MiniGameSelectEvent event = new MiniGameSelectEvent(
                    JOIN_CODE.getValue(),
                    HOST.getName(),
                    List.of(MiniGameType.CARD_GAME, MiniGameType.RACING_GAME));

            sut.updateGames(event);

            final GameSession session = sut.getSession(JOIN_CODE);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(repository.existsByJoinCode(JOIN_CODE)).isTrue();
                softly.assertThat(session.getHost()).isEqualTo(HOST);
                softly.assertThat(session.getSelectedTypes())
                        .containsExactly(MiniGameType.CARD_GAME, MiniGameType.RACING_GAME);
            });
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
            final MiniGameSelectEvent event = new MiniGameSelectEvent(
                    JOIN_CODE.getValue(), HOST.getName(), List.of(MiniGameType.CARD_GAME));

            sut.updateGames(event);

            assertThat(sut.getSession(JOIN_CODE).getSelectedTypes())
                    .containsExactly(MiniGameType.CARD_GAME);
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
