package coffeeshout.admin.room.ui;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.admin.room.domain.RoomPlayer;
import coffeeshout.admin.room.ui.response.RoomPlayerResponse;
import coffeeshout.room.domain.player.PlayerType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RoomPlayerResponse")
class RoomPlayerResponseTest {

    private static final LocalDateTime JOINED_AT = LocalDateTime.of(2026, 9, 6, 12, 0);

    @Test
    void 로그인_사용자는_게스트가_아니고_계정_정보를_함께_싣는다() {
        final RoomPlayer player = new RoomPlayer(1L, "엠제이", PlayerType.HOST, 100L, "mj", "AB12C", JOINED_AT);

        final RoomPlayerResponse response = RoomPlayerResponse.from(player);

        assertThat(response.guest()).isFalse();
        assertThat(response.nickname()).isEqualTo("mj");
        assertThat(response.userCode()).isEqualTo("AB12C");
    }

    @Test
    void userId가_없으면_게스트로_표시한다() {
        // 화면이 userId 의 null 여부를 따지지 않아도 되게 서버가 정한다.
        final RoomPlayer guest = new RoomPlayer(2L, "손님", PlayerType.GUEST, null, null, null, JOINED_AT);

        final RoomPlayerResponse response = RoomPlayerResponse.from(guest);

        assertThat(response.guest()).isTrue();
        assertThat(response.nickname()).isNull();
    }

    @Test
    void 방에서_쓴_이름과_계정_닉네임을_모두_싣는다() {
        // 둘이 다를 수 있다. 문의 대응에서 어느 쪽으로 물어올지 모른다.
        final RoomPlayer player = new RoomPlayer(1L, "방에서쓴이름", PlayerType.GUEST, 100L, "계정닉네임", "AB12C", JOINED_AT);

        final RoomPlayerResponse response = RoomPlayerResponse.from(player);

        assertThat(response.playerName()).isEqualTo("방에서쓴이름");
        assertThat(response.nickname()).isEqualTo("계정닉네임");
    }
}
