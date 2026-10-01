package coffeeshout.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import coffeeshout.fixture.UserFixture;
import coffeeshout.global.nickname.NicknamesCollectedEvent;
import coffeeshout.global.nickname.ProfanityChecker;
import coffeeshout.global.nickname.ProfanityWordBlockedEvent;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserNickname;
import coffeeshout.user.domain.repository.UserRepository;
import java.util.List;
import java.util.Set;
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

    @Mock
    ProfanityChecker profanityChecker;

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
                given(userRepository.findAllByNickname(new UserNickname("나쁜말"))).willReturn(List.of(사용자1, 사용자2));
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

    @Nested
    class 랭킹_닉네임_수집_이벤트를_수신했을_때 {

        @Nested
        class 사전에_걸리는_닉네임이_없으면 {

            @Test
            void 저장소를_조회하지_않는다() {
                userNicknameCleanupService.onNicknamesCollected(new NicknamesCollectedEvent(Set.of("용감한호랑이")));

                then(userRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        class 사전에_걸리는_닉네임이_있으면 {

            @Test
            void 회원_프로필_닉네임을_생성_닉네임으로_교체한다() {
                final User 회원 = UserFixture.저장된_회원(1L, "씨발이닷");
                given(profanityChecker.contains("씨발이닷")).willReturn(true);
                given(userRepository.findAllByNickname(new UserNickname("씨발이닷")))
                        .willReturn(List.of(회원));
                given(nicknameDefaultGenerator.generate()).willReturn("빠른여우");

                userNicknameCleanupService.onNicknamesCollected(new NicknamesCollectedEvent(Set.of("씨발이닷")));

                assertThat(회원.getNickname().value()).isEqualTo("빠른여우");
            }

            @Test
            void 교체한_회원을_저장한다() {
                final User 회원 = UserFixture.저장된_회원(1L, "씨발이닷");
                given(profanityChecker.contains("씨발이닷")).willReturn(true);
                given(userRepository.findAllByNickname(new UserNickname("씨발이닷")))
                        .willReturn(List.of(회원));
                given(nicknameDefaultGenerator.generate()).willReturn("빠른여우");

                userNicknameCleanupService.onNicknamesCollected(new NicknamesCollectedEvent(Set.of("씨발이닷")));

                then(userRepository).should(times(1)).save(회원);
            }

            @Test
            void 사전에_안_걸리는_닉네임은_조회하지_않는다() {
                given(profanityChecker.contains("씨발이닷")).willReturn(true);
                given(profanityChecker.contains("용감한호랑이")).willReturn(false);
                given(userRepository.findAllByNickname(new UserNickname("씨발이닷")))
                        .willReturn(List.of());

                userNicknameCleanupService.onNicknamesCollected(new NicknamesCollectedEvent(Set.of("씨발이닷", "용감한호랑이")));

                then(userRepository).should(never()).findAllByNickname(new UserNickname("용감한호랑이"));
            }
        }

        @Nested
        class 닉네임_길이를_넘는_이름이_섞여_있으면 {

            @Test
            void 그_이름만_건너뛰고_나머지를_교체한다() {
                final User 회원 = UserFixture.저장된_회원(1L, "씨발이닷");
                final String 길이초과이름 = "열한글자닉네임이에요이";
                given(profanityChecker.contains(길이초과이름)).willReturn(true);
                given(profanityChecker.contains("씨발이닷")).willReturn(true);
                given(userRepository.findAllByNickname(new UserNickname("씨발이닷")))
                        .willReturn(List.of(회원));
                given(nicknameDefaultGenerator.generate()).willReturn("빠른여우");

                userNicknameCleanupService.onNicknamesCollected(new NicknamesCollectedEvent(Set.of("씨발이닷", 길이초과이름)));

                assertThat(회원.getNickname().value()).isEqualTo("빠른여우");
            }
        }
    }
}
