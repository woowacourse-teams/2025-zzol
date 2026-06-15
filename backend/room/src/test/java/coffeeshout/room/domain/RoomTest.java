package coffeeshout.room.domain;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.fixture.RouletteFixture;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.domain.roulette.Probability;
import coffeeshout.room.domain.roulette.Roulette;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class RoomTest {

    private final JoinCode joinCode = new JoinCode("ABCD");
    private final Roulette roulette = RouletteFixture.고정_끝값_반환();
    private final PlayerName 호스트_한스 = new PlayerName("한스");
    private final PlayerName 게스트_루키 = new PlayerName("루키");
    private final PlayerName 게스트_꾹이 = new PlayerName("꾹이");
    private final PlayerName 게스트_엠제이 = new PlayerName("엠제이");

    private Room room;

    @BeforeEach
    void setUp() {
        room = new Room(joinCode, 호스트_한스, 0.7);
    }

    @Test
    void 방_생성시_상태는_READY이고_호스트가_추가된다() {
        // given
        // when & then
        assertThat(room.getRoomState()).isEqualTo(RoomState.READY);
        assertThat(room.getHost()).isEqualTo(Player.createHost(호스트_한스));
    }

    @Test
    void READY_상태에서는_플레이어가_참여할_수_있다() {
        // given
        room.joinGuest(게스트_꾹이);

        // when & then
        assertThat(room.getPlayers()).hasSize(2);
    }

    @Test
    void READY_상태가_아니면_참여할_수_없다() {
        // given
        room.joinGuest(게스트_꾹이);
        ReflectionTestUtils.setField(room, "roomState", RoomState.PLAYING);

        // when & then
        assertThatThrownBy(() -> room.joinGuest(게스트_엠제이))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.ROOM_NOT_READY_TO_JOIN);
    }

    @Test
    void 중복된_이름으로_참여할_수_없다() {
        // given
        room.joinGuest(게스트_꾹이);

        // when & then
        assertThatThrownBy(() -> room.joinGuest(게스트_꾹이))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.DUPLICATE_PLAYER_NAME);
    }

    @Test
    void 방이_가득_차면_참여할_수_없다() {
        // given
        for (int i = 1; i < 9; i++) {
            room.joinGuest(new PlayerName("guest" + i));
        }

        // when & then
        assertThatThrownBy(() -> room.joinGuest(new PlayerName("guest9")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.ROOM_FULL);
    }

    @Test
    void 룰렛을_시작하면_상태가_DONE으로_변하고_한_명은_선택된다() {
        // given
        room.joinGuest(게스트_꾹이);
        room.joinGuest(게스트_루키);
        room.joinGuest(게스트_엠제이);

        ReflectionTestUtils.setField(room, "roomState", RoomState.ROULETTE);
        Player host = room.findPlayer(호스트_한스);

        Winner winner = room.spinRoulette(host, roulette);

        // when & then
        assertThat(room.getRoomState()).isEqualTo(RoomState.DONE);
        assertThat(winner.name()).isEqualTo(new PlayerName("엠제이"));
    }

    @Test
    void 룰렛은_호스트만_돌릴_수_있다() {
        // given
        room.joinGuest(게스트_꾹이);
        Player guest = room.findPlayer(게스트_꾹이);

        ReflectionTestUtils.setField(room, "roomState", RoomState.PLAYING);

        // when & then
        assertThatThrownBy(() -> room.spinRoulette(guest, roulette))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 룰렛은_2명_이상이어야_돌릴_수_있다() {
        // given
        Player host = room.findPlayer(호스트_한스);

        // when & then
        assertThatThrownBy(() -> room.spinRoulette(host, roulette))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 룰렛은_게임_중일때만_돌릴_수_있다() {
        // given
        room.joinGuest(게스트_꾹이);
        Player host = room.findPlayer(호스트_한스);

        // when & then
        assertThatThrownBy(() -> room.spinRoulette(host, roulette))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 호스트_판별이_가능하다() {
        // given

        // when & then
        assertThat(room.isHost(Player.createHost(호스트_한스))).isTrue();
        assertThat(room.isHost(Player.createGuest(게스트_꾹이))).isFalse();
    }

    @Test
    void 호스트가_나가면_남은_플레이어_중_랜덤으로_새_호스트가_된다() {
        // given
        room.joinGuest(게스트_루키);
        room.joinGuest(게스트_꾹이);
        room.joinGuest(게스트_엠제이);

        Player originalHost = room.getHost();
        assertThat(originalHost.getName()).isEqualTo(호스트_한스);
        assertThat(room.getPlayers()).hasSize(4);

        // when
        boolean removed = room.removePlayer(호스트_한스);

        // then
        assertThat(removed).isTrue();
        assertThat(room.getPlayers()).hasSize(3);

        Player newHost = room.getHost();
        assertThat(newHost.getName()).isNotEqualTo(호스트_한스);
        assertThat(newHost.getName()).isIn(게스트_루키, 게스트_꾹이, 게스트_엠제이);
    }

    @Test
    void 호스트가_나가고_남은_플레이어가_없으면_호스트_승격이_일어나지_않는다() {
        // given
        assertThat(room.getPlayers()).hasSize(1);
        Player originalHost = room.getHost();
        assertThat(originalHost.getName()).isEqualTo(호스트_한스);

        // when
        boolean removed = room.removePlayer(호스트_한스);

        // then
        assertThat(removed).isTrue();
        assertThat(room.getPlayers()).isEmpty();
        // 원래 호스트 객체는 그대로 유지됨 (빈 방이므로)
        assertThat(room.getHost()).isEqualTo(originalHost);
    }

    @Test
    void 게스트가_나가면_호스트는_그대로다() {
        // given
        room.joinGuest(게스트_루키);
        room.joinGuest(게스트_꾹이);

        Player originalHost = room.getHost();
        assertThat(originalHost.getName()).isEqualTo(호스트_한스);
        assertThat(room.getPlayers()).hasSize(3);

        // when
        boolean removed = room.removePlayer(게스트_루키);

        // then
        assertThat(removed).isTrue();
        assertThat(room.getPlayers()).hasSize(2);
        assertThat(room.getHost()).isEqualTo(originalHost);
        assertThat(room.getHost().getName()).isEqualTo(호스트_한스);
    }

    @Test
    void 존재하지_않는_플레이어_제거시_false_반환() {
        // given
        PlayerName 존재하지않는플레이어 = new PlayerName("없는놈");

        // when
        boolean removed = room.removePlayer(존재하지않는플레이어);

        // then
        assertThat(removed).isFalse();
        assertThat(room.getPlayers()).hasSize(1);
        assertThat(room.getHost().getName()).isEqualTo(호스트_한스);
    }

    @Nested
    class 가중치_설정 {

        @Test
        void 호스트는_READY_상태에서_가중치를_변경할_수_있다() {
            // when
            room.updateAdjustmentWeight(호스트_한스, 0.5);

            // then
            assertThat(room.getAdjustmentWeight()).isEqualTo(0.5);
        }

        @Test
        void 경계값_0_1과_0_9는_정상_변경된다() {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThatCode(() -> room.updateAdjustmentWeight(호스트_한스, 0.1)).doesNotThrowAnyException();
                softly.assertThatCode(() -> room.updateAdjustmentWeight(호스트_한스, 0.9)).doesNotThrowAnyException();
            });
        }

        @Test
        void 비호스트는_가중치를_변경할_수_없다() {
            // given
            room.joinGuest(게스트_꾹이);

            // when & then
            assertCoffeeShoutException(
                    () -> room.updateAdjustmentWeight(게스트_꾹이, 0.5),
                    RoomErrorCode.NOT_HOST
            );
        }

        @Test
        void READY_상태가_아니면_가중치를_변경할_수_없다() {
            // given
            ReflectionTestUtils.setField(room, "roomState", RoomState.PLAYING);

            // when & then
            assertCoffeeShoutException(
                    () -> room.updateAdjustmentWeight(호스트_한스, 0.5),
                    RoomErrorCode.ROOM_NOT_READY_TO_UPDATE
            );
        }

        @ParameterizedTest
        @ValueSource(doubles = {0.0, 0.09, -0.1, -1.0})
        void 가중치가_0_1_미만이면_예외가_발생한다(double invalidWeight) {
            assertCoffeeShoutException(
                    () -> room.updateAdjustmentWeight(호스트_한스, invalidWeight),
                    RoomErrorCode.INVALID_ADJUSTMENT_WEIGHT
            );
        }

        @ParameterizedTest
        @ValueSource(doubles = {0.91, 1.0, 2.0})
        void 가중치가_0_9_초과이면_예외가_발생한다(double invalidWeight) {
            assertCoffeeShoutException(
                    () -> room.updateAdjustmentWeight(호스트_한스, invalidWeight),
                    RoomErrorCode.INVALID_ADJUSTMENT_WEIGHT
            );
        }

        @ParameterizedTest
        @ValueSource(doubles = {0.0, 0.09, -0.1, -1.0})
        void 생성_시_가중치가_0_1_미만이면_예외가_발생한다(double invalidWeight) {
            assertCoffeeShoutException(
                    () -> new Room(joinCode, 호스트_한스, invalidWeight),
                    RoomErrorCode.INVALID_ADJUSTMENT_WEIGHT
            );
        }

        @ParameterizedTest
        @ValueSource(doubles = {0.91, 1.0, 2.0})
        void 생성_시_가중치가_0_9_초과이면_예외가_발생한다(double invalidWeight) {
            assertCoffeeShoutException(
                    () -> new Room(joinCode, 호스트_한스, invalidWeight),
                    RoomErrorCode.INVALID_ADJUSTMENT_WEIGHT
            );
        }

        @Test
        void 생성_시_경계값_0_1과_0_9는_정상_생성된다() {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThatCode(() -> new Room(joinCode, 호스트_한스, 0.1)).doesNotThrowAnyException();
                softly.assertThatCode(() -> new Room(joinCode, 호스트_한스, 0.9)).doesNotThrowAnyException();
            });
        }
    }

    @Nested
    class 호스트_검증_validateHost {

        @Test
        void 호스트면_예외가_발생하지_않는다() {
            assertThatCode(() -> room.validateHost(호스트_한스.value()))
                    .doesNotThrowAnyException();
        }

        @Test
        void 호스트가_아니면_NOT_HOST_예외가_발생한다() {
            // given
            room.joinGuest(게스트_꾹이);

            // when & then
            assertCoffeeShoutException(
                    () -> room.validateHost(게스트_꾹이.value()),
                    RoomErrorCode.NOT_HOST
            );
        }
    }

    @Nested
    class 게임_시작_검증_validateStartable {

        @Test
        void 호스트_전원준비_2명이상_READY면_통과하고_상태를_바꾸지_않는다() {
            // given
            room.joinGuest(게스트_꾹이);
            room.findPlayer(게스트_꾹이).updateReadyState(true);

            // when & then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThatCode(() -> room.validateStartable(호스트_한스.value()))
                        .doesNotThrowAnyException();
                softly.assertThat(room.getRoomState()).isEqualTo(RoomState.READY);
            });
        }

        @Test
        void 호스트가_아니면_예외가_발생한다() {
            // given
            room.joinGuest(게스트_꾹이);
            room.findPlayer(게스트_꾹이).updateReadyState(true);

            // when & then
            assertThatThrownBy(() -> room.validateStartable(게스트_꾹이.value()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("호스트가 게임을 시작할 수 있습니다.");
        }

        @Test
        void 전원이_준비되지_않으면_예외가_발생한다() {
            // given
            room.joinGuest(게스트_꾹이);

            // when & then
            assertThatThrownBy(() -> room.validateStartable(호스트_한스.value()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("모든 플레이어가 준비 완료해야합니다.");
        }

        @Test
        void 플레이어가_2명_미만이면_예외가_발생한다() {
            // when & then (호스트 혼자)
            assertThatThrownBy(() -> room.validateStartable(호스트_한스.value()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("게임을 시작하려면 플레이어가 2명 이상이어야 합니다.");
        }
    }

    @Nested
    class 상태_전이_markPlaying {

        @Test
        void markPlaying은_방_상태를_PLAYING으로_바꾼다() {
            // when
            room.markPlaying();

            // then
            assertThat(room.getRoomState()).isEqualTo(RoomState.PLAYING);
        }
    }

    @Nested
    class 게이머_변환_getGamers {

        @Test
        void getGamers는_플레이어를_Gamer로_변환해_반환한다() {
            // given
            room.joinGuest(게스트_꾹이);

            // when
            List<Gamer> gamers = room.getGamers();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(gamers).hasSize(2);
                softly.assertThat(gamers).extracting(Gamer::getName)
                        .containsExactlyInAnyOrder(호스트_한스.value(), 게스트_꾹이.value());
            });
        }
    }

    @Nested
    class 게임_결과_적용_applyGameResult {

        @Test
        void 순위_맵으로_확률을_조정하고_SCORE_BOARD로_전이한다() {
            // given
            room.joinGuest(게스트_루키);
            room.joinGuest(게스트_꾹이);
            room.joinGuest(게스트_엠제이);
            Map<PlayerName, Integer> rankByPlayer = Map.of(
                    호스트_한스, 1,
                    게스트_루키, 2,
                    게스트_꾹이, 3,
                    게스트_엠제이, 4
            );

            // when
            room.applyGameResult(rankByPlayer, 5);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(room.getRoomState()).isEqualTo(RoomState.SCORE_BOARD);
                softly.assertThat(room.findPlayer(호스트_한스).getProbability())
                        .isEqualTo(new Probability((int) (2500 - 500 * 0.7)));
                softly.assertThat(room.findPlayer(게스트_엠제이).getProbability())
                        .isEqualTo(new Probability((int) (2500 + 500 * 0.7)));
            });
        }
    }
}
