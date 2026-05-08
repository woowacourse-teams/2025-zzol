package coffeeshout.room.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.UserFixture;
import coffeeshout.global.ServiceTest;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.user.domain.AuthenticatedUser;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.UserRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RoomServiceMemberTest extends ServiceTest {

    @Autowired
    RoomService roomService;

    @Autowired
    UserRepository userRepository;

    User 저장된_엠제이;
    AuthenticatedUser 엠제이_인증;

    @BeforeEach
    void setUp() {
        저장된_엠제이 = userRepository.save(UserFixture.회원_엠제이());
        엠제이_인증 = new AuthenticatedUser(저장된_엠제이.getId(), 저장된_엠제이.getUserCode().value());
    }

    @Nested
    class 회원이_방을_생성할_때 {

        @Test
        void 요청_닉네임_무시하고_프로필_닉네임으로_방이_생성된다() {
            final Room room = roomService.createRoom(엠제이_인증).room();

            final Player host = room.findPlayer(new PlayerName("엠제이"));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(host).isNotNull();
                softly.assertThat(host.getUserId()).isEqualTo(저장된_엠제이.getId());
            });
        }
    }

    @Nested
    class 익명이_방을_생성할_때 {

        @Test
        void 요청_닉네임으로_방이_생성된다() {
            final Room room = roomService.createRoom("익명호스트").room();

            final Player host = room.findPlayer(new PlayerName("익명호스트"));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(host).isNotNull();
                softly.assertThat(host.getUserId()).isNull();
            });
        }
    }
}
