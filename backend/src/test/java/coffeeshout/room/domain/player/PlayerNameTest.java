package coffeeshout.room.domain.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.room.domain.RoomErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PlayerNameTest {

    @ParameterizedTest
    @ValueSource(strings = {"김철수", "player1", "참가자", "a", "1234567890"})
    void 플레이어_이름이_유효하다면_생성된다(String name) {
        // given & when
        PlayerName playerName = new PlayerName(name);

        // then
        assertThat(playerName.value()).isEqualTo(name);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
    void 플레이어_이름이_공백이면_예외를_발생한다(String name) {
        // given & when & then
        assertThatThrownBy(() -> new PlayerName(name))
                .isInstanceOf(InvalidArgumentException.class)
                .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.PLAYER_NAME_BLANK);
    }

    @Test
    void 플레이어_이름이_10자를_초과하면_예외를_발생한다() {
        // given
        String longName = "12345678901"; // 11자

        // when & then
        assertThatThrownBy(() -> new PlayerName(longName))
                .isInstanceOf(InvalidArgumentException.class)
                .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.PLAYER_NAME_TOO_LONG);
    }
}