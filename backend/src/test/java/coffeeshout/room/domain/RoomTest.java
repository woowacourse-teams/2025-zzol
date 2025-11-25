package coffeeshout.room.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.card.CardGameRandomDeckGenerator;
import coffeeshout.fixture.MenuFixture;
import coffeeshout.fixture.MiniGameDummy;
import coffeeshout.fixture.RouletteFixture;
import coffeeshout.global.exception.custom.InvalidStateException;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.menu.MenuTemperature;
import coffeeshout.room.domain.menu.SelectedMenu;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.domain.roulette.Roulette;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        room = new Room(joinCode, 호스트_한스, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));
    }

    @Test
    void 방_생성시_상태는_READY이고_호스트가_추가된다() {
        // given
        // when & then
        assertThat(room.getRoomState()).isEqualTo(RoomState.READY);
        assertThat(room.getHost()).isEqualTo(Player.createHost(
                호스트_한스, new SelectedMenu(
                        MenuFixture.아메리카노(),
                        MenuTemperature.ICE
                )
        ));
    }

    @Test
    void READY_상태에서는_플레이어가_참여할_수_있다() {
        // given
        room.joinGuest(게스트_꾹이, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));

        // when & then
        assertThat(room.getPlayers()).hasSize(2);
    }

    @Test
    void READY_상태가_아니면_참여할_수_없다() {
        // given
        room.joinGuest(게스트_꾹이, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));
        ReflectionTestUtils.setField(room, "roomState", RoomState.PLAYING);

        // when & then
        assertThatThrownBy(() -> room.joinGuest(게스트_엠제이, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE)))
                .isInstanceOf(InvalidStateException.class)
                .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.ROOM_NOT_READY_TO_JOIN);
    }

    @Test
    void 중복된_이름으로_참여할_수_없다() {
        // given
        room.joinGuest(게스트_꾹이, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));

        // when & then
        assertThatThrownBy(() -> room.joinGuest(게스트_꾹이, new SelectedMenu(MenuFixture.라떼(), MenuTemperature.ICE)))
                .isInstanceOf(InvalidStateException.class)
                .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.DUPLICATE_PLAYER_NAME);
    }

    @Test
    void 방이_가득_차면_참여할_수_없다() {
        // given
        for (int i = 1; i < 9; i++) {
            room.joinGuest(new PlayerName("guest" + i), new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));
        }

        // when & then
        assertThatThrownBy(() -> room.joinGuest(
                new PlayerName("guest9"), new SelectedMenu(
                        MenuFixture.아메리카노(),
                        MenuTemperature.ICE
                )
        ))
                .isInstanceOf(InvalidStateException.class)
                .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.ROOM_FULL);
    }

    @Test
    void 미니게임은_5개_이하여야_한다() {
        // given
        List<Playable> miniGames = new LinkedList<>(List.of(
                new MiniGameDummy(),
                new MiniGameDummy(),
                new MiniGameDummy(),
                new MiniGameDummy()
        ));
        ReflectionTestUtils.setField(room, "miniGames", miniGames);

        // when
        room.addMiniGame(호스트_한스, new MiniGameDummy());

        // then
        assertThat(room.getMiniGames()).hasSize(5);
    }

    @Test
    void 미니게임이_6개_이상이면_예외가_발생한다() {
        // given
        List<Playable> miniGames = new LinkedList<>(List.of(
                new MiniGameDummy(),
                new MiniGameDummy(),
                new MiniGameDummy(),
                new MiniGameDummy(),
                new MiniGameDummy(),
                new MiniGameDummy()
        ));

        ReflectionTestUtils.setField(room, "miniGames", miniGames);

        MiniGameDummy miniGameDummy = new MiniGameDummy();
        // when & then
        assertThatThrownBy(() -> room.addMiniGame(호스트_한스, miniGameDummy))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 미니게임을_제거한다() {
        // given
        CardGame cardGame = new CardGame(new CardGameRandomDeckGenerator(), 1234L);
        room.addMiniGame(호스트_한스, cardGame);

        // when
        room.removeMiniGame(호스트_한스, cardGame);

        // then
        assertThat(room.getMiniGames()).isEmpty();
    }

    @Test
    void 해당_미니게임이_없을_때_제거하면_예외를_발생한다() {
        // given
        MiniGameDummy miniGameDummy = new MiniGameDummy();

        // when & then
        assertThatThrownBy(() -> {
            room.removeMiniGame(호스트_한스, miniGameDummy);
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 룰렛을_시작하면_상태가_DONE으로_변하고_한_명은_선택된다() {
        // given
        room.joinGuest(게스트_꾹이, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));
        room.joinGuest(게스트_루키, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));
        room.joinGuest(게스트_엠제이, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));

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
        room.joinGuest(게스트_꾹이, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));
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
        room.joinGuest(게스트_꾹이, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));
        Player host = room.findPlayer(호스트_한스);

        // when & then
        assertThatThrownBy(() -> room.spinRoulette(host, roulette))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 호스트_판별이_가능하다() {
        // given

        // when & then
        assertThat(room.isHost(Player.createHost(
                호스트_한스,
                new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE)
        ))).isTrue();
        assertThat(room.isHost(Player.createGuest(
                게스트_꾹이,
                new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE)
        ))).isFalse();
    }

    @Test
    void 호스트가_아니면_미니게임을_추가할_수_없다() {
        // given
        room.joinGuest(게스트_꾹이, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));
        MiniGameDummy miniGameDummy = new MiniGameDummy();

        // when & then
        assertThatThrownBy(() -> room.addMiniGame(게스트_꾹이, miniGameDummy))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 호스트가_아니면_미니게임을_제거할_수_없다() {
        // given
        CardGame cardGame = new CardGame(new CardGameRandomDeckGenerator(), 1234L);
        room.addMiniGame(호스트_한스, cardGame);
        room.joinGuest(게스트_꾹이, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));

        // when & then
        assertThatThrownBy(() -> room.removeMiniGame(게스트_꾹이, cardGame))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 미니게임을_시작한다() {
        // given
        CardGame cardGame = new CardGame(new CardGameRandomDeckGenerator(), 1234L);
        room.addMiniGame(호스트_한스, cardGame);
        room.joinGuest(게스트_꾹이, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));
        Player host = room.getHost();
        Player guest = room.findPlayer(게스트_꾹이);

        // when
        guest.updateReadyState(true);
        Playable playable = room.startNextGame(host.getName().value());

        // then
        assertThat(playable.getMiniGameType()).isEqualTo(MiniGameType.CARD_GAME);
    }

    @Test
    void 게임_시작_시_모든_플레이어가_레디_상태가_아니면_예외가_발생한다() {
        // given
        CardGame cardGame = new CardGame(new CardGameRandomDeckGenerator(), 1234L);
        room.addMiniGame(호스트_한스, cardGame);
        room.joinGuest(게스트_꾹이, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));
        Player host = room.getHost();

        // when & then
        assertThatThrownBy(() -> room.startNextGame(host.getName().value()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("모든 플레이어가 준비완료해야합니다.");
    }

    @Test
    void 호스트가_나가면_남은_플레이어_중_랜덤으로_새_호스트가_된다() {
        // given
        room.joinGuest(게스트_루키, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));
        room.joinGuest(게스트_꾹이, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));
        room.joinGuest(게스트_엠제이, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));

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
        room.joinGuest(게스트_루키, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));
        room.joinGuest(게스트_꾹이, new SelectedMenu(MenuFixture.아메리카노(), MenuTemperature.ICE));

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
}
