package coffeeshout.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import coffeeshout.fixture.UserFixture;
import coffeeshout.room.infra.event.ProfanityWordBlockedEvent;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserNickname;
import coffeeshout.user.domain.repository.UserRepository;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserNicknameCleanupServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    NicknameDefaultGenerator nicknameDefaultGenerator;

    @InjectMocks
    UserNicknameCleanupService userNicknameCleanupService;

    @Nested
    class 비속어_차단_이벤트를_수신했을_때 {

        @Nested
        class 해당_닉네임을_가진_사용자가_없으면 {

            @BeforeEach
            void setUp() {
                given(userRepository.findAllByNickname(any(UserNickname.class))).willReturn(List.of());
            }

            @Test
            void 저장소에_저장하지_않는다() {
                userNicknameCleanupService.onProfanityWordBlocked(new ProfanityWordBlockedEvent("나쁜말"));

                then(userRepository).should(never()).save(any());
            }
        }

        @Nested
        class 해당_닉네임을_가진_사용자가_있으면 {

            User 사용자1;
            User 사용자2;

            @BeforeEach
            void setUp() {
                사용자1 = UserFixture.저장된_회원(1L, "나쁜말");
                사용자2 = UserFixture.저장된_회원(2L, "나쁜말");
                given(userRepository.findAllByNickname(new UserNickname("나쁜말")))
                        .willReturn(List.of(사용자1, 사용자2));
                given(nicknameDefaultGenerator.generate()).willReturn("용감한호랑이", "귀여운여우");
                given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            }

            @Test
            void 사용자_수만큼_저장한다() {
                userNicknameCleanupService.onProfanityWordBlocked(new ProfanityWordBlockedEvent("나쁜말"));

                then(userRepository).should(times(2)).save(any(User.class));
            }

            @Test
            void 닉네임이_랜덤_생성된_값으로_교체된다() {
                userNicknameCleanupService.onProfanityWordBlocked(new ProfanityWordBlockedEvent("나쁜말"));

                final ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
                then(userRepository).should(times(2)).save(captor.capture());

                final List<String> savedNicknames = captor.getAllValues().stream()
                        .map(u -> u.getNickname().value())
                        .toList();

                assertThat(savedNicknames).containsExactlyInAnyOrder("용감한호랑이", "귀여운여우");
            }

            @Test
            void 원래_닉네임이_차단된_단어로_남지_않는다() {
                userNicknameCleanupService.onProfanityWordBlocked(new ProfanityWordBlockedEvent("나쁜말"));

                assertThat(사용자1.getNickname().value()).isNotEqualTo("나쁜말");
                assertThat(사용자2.getNickname().value()).isNotEqualTo("나쁜말");
            }
        }

        @Nested
        class 닉네임_길이_초과_단어이면 {

            @Test
            void 저장소를_조회하지_않는다() {
                final String 길이초과단어 = "열한글자닉네임이에요이";

                userNicknameCleanupService.onProfanityWordBlocked(new ProfanityWordBlockedEvent(길이초과단어));

                then(userRepository).should(never()).findAllByNickname(any());
                then(userRepository).should(never()).save(any());
            }
        }
    }
}
