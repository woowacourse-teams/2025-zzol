package coffeeshout.minigame.domain;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.StubPlayable;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.Playable;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GameSessionTest {

    private static final JoinCode JOIN_CODE = new JoinCode("ABCD");
    private static final Gamer HOST = Gamer.guest("호스트");
    private static final Gamer GUEST = Gamer.guest("게스트");

    private GameSession session;

    @BeforeEach
    void setUp() {
        session = new GameSession(JOIN_CODE, HOST);
    }

    private Playable game(MiniGameType type) {
        return new StubPlayable(type);
    }

    @Nested
    @DisplayName("게임 목록 교체(replaceGames)")
    class ReplaceGames {

        @Test
        @DisplayName("호스트가 교체하면 대기열이 갱신된다")
        void 호스트가_교체하면_대기열이_갱신된다() {
            session.replaceGames(HOST, List.of(game(MiniGameType.CARD_GAME), game(MiniGameType.RACING_GAME)));

            assertThat(session.getSelectedTypes())
                    .containsExactly(MiniGameType.CARD_GAME, MiniGameType.RACING_GAME);
        }

        @Test
        @DisplayName("호스트가 아니면 NOT_HOST 예외가 발생한다")
        void 호스트가_아니면_NOT_HOST_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> session.replaceGames(GUEST, List.of(game(MiniGameType.CARD_GAME))),
                    GameSessionErrorCode.NOT_HOST);
        }

        @Test
        @DisplayName("이름이 같으면 userId가 달라도 호스트로 인정한다")
        void 이름이_같으면_userId가_달라도_호스트로_인정한다() {
            final Gamer sameNameLoggedIn = Gamer.loggedIn("호스트", 999L);

            session.replaceGames(sameNameLoggedIn, List.of(game(MiniGameType.CARD_GAME)));

            assertThat(session.getSelectedTypes()).containsExactly(MiniGameType.CARD_GAME);
        }

        @Test
        @DisplayName("동일 게임 타입을 중복 선택하면 DUPLICATE_GAME 예외가 발생한다")
        void 동일_게임_타입_중복이면_DUPLICATE_GAME_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> session.replaceGames(HOST, List.of(game(MiniGameType.CARD_GAME), game(MiniGameType.CARD_GAME))),
                    GameSessionErrorCode.DUPLICATE_GAME);
        }

        @Test
        @DisplayName("게임 5개는 허용된다(경계값)")
        void 게임_5개는_허용된다() {
            final List<Playable> fiveGames = List.of(
                    game(MiniGameType.CARD_GAME),
                    game(MiniGameType.RACING_GAME),
                    game(MiniGameType.SPEED_TOUCH),
                    game(MiniGameType.BLIND_TIMER),
                    game(MiniGameType.BLOCK_STACKING));

            session.replaceGames(HOST, fiveGames);

            assertThat(session.roundCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("게임 6개를 선택하면 TOO_MANY_GAMES 예외가 발생한다(경계값)")
        void 게임_6개를_선택하면_TOO_MANY_GAMES_예외가_발생한다() {
            final List<Playable> sixGames = List.of(
                    game(MiniGameType.CARD_GAME),
                    game(MiniGameType.RACING_GAME),
                    game(MiniGameType.SPEED_TOUCH),
                    game(MiniGameType.BLIND_TIMER),
                    game(MiniGameType.BLOCK_STACKING),
                    game(MiniGameType.LADDER_GAME));

            assertCoffeeShoutException(
                    () -> session.replaceGames(HOST, sixGames),
                    GameSessionErrorCode.TOO_MANY_GAMES);
        }

        @Test
        @DisplayName("게임 진행 중(PLAYING)에는 교체할 수 없어 GAME_IN_PROGRESS 예외가 발생한다")
        void 게임_진행_중에는_GAME_IN_PROGRESS_예외가_발생한다() {
            session.replaceGames(HOST, List.of(game(MiniGameType.CARD_GAME)));
            session.startNextGame(HOST, List.of(HOST, GUEST));

            assertCoffeeShoutException(
                    () -> session.replaceGames(HOST, List.of(game(MiniGameType.RACING_GAME))),
                    GameSessionErrorCode.GAME_IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("게임 시작(startNextGame)")
    class StartNextGame {

        @Test
        @DisplayName("READY에서 시작하면 PLAYING으로 전이하고 시작 게임을 반환한다")
        void READY에서_시작하면_PLAYING으로_전이한다() {
            session.replaceGames(HOST, List.of(game(MiniGameType.CARD_GAME)));

            final Playable started = session.startNextGame(HOST, List.of(HOST, GUEST));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(session.getStatus()).isEqualTo(GameSessionStatus.PLAYING);
                softly.assertThat(started.getMiniGameType()).isEqualTo(MiniGameType.CARD_GAME);
            });
        }

        @Test
        @DisplayName("전달한 플레이어 목록으로 setUp이 호출된다")
        void 전달한_플레이어_목록으로_setUp이_호출된다() {
            final StubPlayable card = new StubPlayable(MiniGameType.CARD_GAME);
            session.replaceGames(HOST, List.of(card));
            final List<Gamer> gamers = List.of(HOST, GUEST);

            session.startNextGame(HOST, gamers);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(card.isSetUpCalled()).isTrue();
                softly.assertThat(card.getSetUpGamers()).isEqualTo(gamers);
            });
        }

        @Test
        @DisplayName("시작한 게임은 완료 목록으로 이동해 findCompletedGame으로 조회된다")
        void 시작한_게임은_완료_목록으로_이동한다() {
            session.replaceGames(HOST, List.of(game(MiniGameType.CARD_GAME)));

            session.startNextGame(HOST, List.of(HOST, GUEST));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(session.getSelectedTypes()).isEmpty();
                softly.assertThat(session.findCompletedGame(MiniGameType.CARD_GAME).getMiniGameType())
                        .isEqualTo(MiniGameType.CARD_GAME);
            });
        }

        @Test
        @DisplayName("호스트가 아니면 NOT_HOST 예외가 발생한다")
        void 호스트가_아니면_NOT_HOST_예외가_발생한다() {
            session.replaceGames(HOST, List.of(game(MiniGameType.CARD_GAME)));

            assertCoffeeShoutException(
                    () -> session.startNextGame(GUEST, List.of(HOST, GUEST)),
                    GameSessionErrorCode.NOT_HOST);
        }

        @Test
        @DisplayName("대기 게임이 없으면 NO_PENDING_GAMES 예외가 발생한다")
        void 대기_게임이_없으면_NO_PENDING_GAMES_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> session.startNextGame(HOST, List.of(HOST, GUEST)),
                    GameSessionErrorCode.NO_PENDING_GAMES);
        }
    }

    @Nested
    @DisplayName("게임 종료(finishCurrentGame)")
    class FinishCurrentGame {

        @Test
        @DisplayName("대기열이 남아 있으면 READY로 복귀한다")
        void 대기열이_남아_있으면_READY로_복귀한다() {
            session.replaceGames(HOST, List.of(game(MiniGameType.CARD_GAME), game(MiniGameType.RACING_GAME)));
            session.startNextGame(HOST, List.of(HOST, GUEST));

            session.finishCurrentGame();

            assertThat(session.getStatus()).isEqualTo(GameSessionStatus.READY);
        }

        @Test
        @DisplayName("대기열이 소진되면 DONE으로 전이한다")
        void 대기열이_소진되면_DONE으로_전이한다() {
            session.replaceGames(HOST, List.of(game(MiniGameType.CARD_GAME)));
            session.startNextGame(HOST, List.of(HOST, GUEST));

            session.finishCurrentGame();

            assertThat(session.getStatus()).isEqualTo(GameSessionStatus.DONE);
        }
    }

    @Nested
    @DisplayName("완료 게임 조회(findCompletedGame)")
    class FindCompletedGame {

        @Test
        @DisplayName("완료 목록에 없는 타입을 조회하면 GAME_NOT_FOUND 예외가 발생한다")
        void 완료_목록에_없는_타입을_조회하면_GAME_NOT_FOUND_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> session.findCompletedGame(MiniGameType.LADDER_GAME),
                    GameSessionErrorCode.GAME_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("라운드 수 불변식(roundCount)")
    class RoundCount {

        @Test
        @DisplayName("선택한 게임 총수는 전부 완료될 때까지 매 단계 일정하게 유지된다")
        void 선택한_게임_총수는_매_단계_일정하게_유지된다() {
            final int selectedCount = 3;
            session.replaceGames(HOST, List.of(
                    game(MiniGameType.CARD_GAME),
                    game(MiniGameType.RACING_GAME),
                    game(MiniGameType.SPEED_TOUCH)));

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(session.roundCount()).as("선택 직후").isEqualTo(selectedCount);

            // 1라운드
            session.startNextGame(HOST, List.of(HOST, GUEST));
            softly.assertThat(session.roundCount()).as("1게임 시작 후").isEqualTo(selectedCount);
            session.finishCurrentGame();
            softly.assertThat(session.roundCount()).as("1게임 종료 후").isEqualTo(selectedCount);
            softly.assertThat(session.getStatus()).as("1게임 종료 후 상태").isEqualTo(GameSessionStatus.READY);

            // 2라운드
            session.startNextGame(HOST, List.of(HOST, GUEST));
            softly.assertThat(session.roundCount()).as("2게임 시작 후").isEqualTo(selectedCount);
            session.finishCurrentGame();
            softly.assertThat(session.roundCount()).as("2게임 종료 후").isEqualTo(selectedCount);

            // 3라운드 (마지막)
            session.startNextGame(HOST, List.of(HOST, GUEST));
            softly.assertThat(session.roundCount()).as("3게임 시작 후").isEqualTo(selectedCount);
            session.finishCurrentGame();
            softly.assertThat(session.roundCount()).as("3게임 종료 후").isEqualTo(selectedCount);
            softly.assertThat(session.getStatus()).as("전부 종료 후 상태").isEqualTo(GameSessionStatus.DONE);

            softly.assertAll();
        }
    }
}
