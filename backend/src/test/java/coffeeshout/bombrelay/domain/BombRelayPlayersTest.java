package coffeeshout.bombrelay.domain;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.room.domain.player.Player;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BombRelayPlayersTest {

    @Nested
    class 라운드_수_계산 {

        @Test
        void 두명이면_1라운드() {
            final BombRelayPlayers players = new BombRelayPlayers(
                    List.of(PlayerFixture.게스트한스(), PlayerFixture.게스트꾹이()));
            assertThat(players.calculateMaxRounds()).isEqualTo(1);
        }

        @Test
        void 세명이면_2라운드() {
            final BombRelayPlayers players = new BombRelayPlayers(
                    List.of(PlayerFixture.게스트한스(), PlayerFixture.게스트꾹이(), PlayerFixture.게스트루키()));
            assertThat(players.calculateMaxRounds()).isEqualTo(2);
        }

        @Test
        void 네명_이상이면_3라운드() {
            final List<Player> fourPlayers = List.of(
                    PlayerFixture.게스트한스(), PlayerFixture.게스트꾹이(),
                    PlayerFixture.게스트루키(), PlayerFixture.게스트엠제이());
            final BombRelayPlayers players = new BombRelayPlayers(fourPlayers);
            assertThat(players.calculateMaxRounds()).isEqualTo(3);
        }
    }

    @Nested
    class 생존자_관리 {

        @Test
        void 초기에는_전원_생존() {
            final BombRelayPlayers players = new BombRelayPlayers(
                    List.of(PlayerFixture.게스트한스(), PlayerFixture.게스트꾹이(), PlayerFixture.게스트루키()));
            assertThat(players.survivorCount()).isEqualTo(3);
        }

        @Test
        void 탈락하면_생존자에서_제외된다() {
            final BombRelayPlayers players = new BombRelayPlayers(
                    List.of(PlayerFixture.게스트한스(), PlayerFixture.게스트꾹이(), PlayerFixture.게스트루키()));

            players.getAll().get(0).eliminate(1);

            assertThat(players.survivorCount()).isEqualTo(2);
            assertThat(players.getSurvivors()).hasSize(2);
        }
    }

    @Nested
    class 턴_인덱스 {

        @Test
        void turnIndex는_생존자_내에서_순환한다() {
            final BombRelayPlayers players = new BombRelayPlayers(
                    List.of(PlayerFixture.게스트한스(), PlayerFixture.게스트꾹이(), PlayerFixture.게스트루키()));

            final BombRelayPlayer first = players.getByTurnIndex(0);
            final BombRelayPlayer fourth = players.getByTurnIndex(3);

            // 3명이니까 index 0과 3은 같은 플레이어
            assertThat(first).isEqualTo(fourth);
        }
    }
}
