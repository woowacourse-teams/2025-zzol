package coffeeshout.profanity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.infra.persistence.audit.NicknameAuditJpaRepository;
import coffeeshout.support.ServiceTest;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

class NicknameExposureServiceTest extends ServiceTest {

    @Autowired
    private NicknameExposureService exposureService;

    @Autowired
    private NicknameAuditJpaRepository auditRepository;

    @Autowired
    private ProfanityWordManagementService wordManagementService;

    @Nested
    class 검열_상태별_공개 {

        @ParameterizedTest
        @EnumSource(
                value = NicknameAuditStatus.class,
                names = {"CLEAN", "ALLOWED"})
        void 판정이_CLEAN_ALLOWED면_공개한다(NicknameAuditStatus status) {
            검열_행(status, "통과닉");

            assertThat(exposureService.findRevealable(List.of("통과닉"))).containsExactly("통과닉");
        }

        @ParameterizedTest
        @EnumSource(
                value = NicknameAuditStatus.class,
                names = {"CLEAN", "ALLOWED"},
                mode = EnumSource.Mode.EXCLUDE)
        void 판정이_CLEAN_ALLOWED가_아니면_가린다(NicknameAuditStatus status) {
            검열_행(status, "미통과닉");

            assertThat(exposureService.findRevealable(List.of("미통과닉"))).isEmpty();
        }

        @Test
        void 상태가_다른_행이_섞여_있으면_하나라도_통과가_아니면_가린다() {
            // 유니크 제약이 (player_name, status)라 같은 닉네임에 CLEAN과 FLAGGED가 함께 있을 수 있다.
            검열_행(NicknameAuditStatus.CLEAN, "섞인닉");
            검열_행(NicknameAuditStatus.FLAGGED, "섞인닉");

            assertThat(exposureService.findRevealable(List.of("섞인닉"))).isEmpty();
        }

        @Test
        void 대소문자만_다른_검열_행도_같은_닉네임의_판정으로_본다() {
            // player_name 콜레이션은 대소문자를 구분하지 않는다. register()의 existsByNickname도 같은 기준이라
            // "abc"는 "ABC" 행이 있으면 자기 행을 따로 갖지 못한다. Java 쪽이 정확히 일치만 보면 영영 가려진다.
            검열_행(NicknameAuditStatus.CLEAN, "ZzolKing");

            assertThat(exposureService.findRevealable(List.of("zzolking"))).containsExactly("zzolking");
        }

        @Test
        void 끝_공백만_다른_CLEAN_행은_공개되고_이벤트를_내지_않는다() {
            // player_name 콜레이션(utf8mb4_unicode_ci)은 PAD SPACE라 끝 공백을 무시한다. IN 쿼리가 "철수 "로 "철수" 행을
            // 돌려주는데 Java 쪽이 공백까지 비교하면 행 없음으로 분류된다. 그러면 register()의 existsByNickname은 행이
            // 있다고 보고 INSERT를 건너뛰어, 닉네임이 영영 가려지고 조회마다 이벤트가 다시 나간다.
            검열_행(NicknameAuditStatus.CLEAN, "철수");

            assertThat(exposureService.findRevealable(List.of("철수 "))).containsExactly("철수 ");
            then(eventPublisher).should(never()).publishEvent(any(Object.class));
        }

        @Test
        void 공개_여부는_조회한_닉네임마다_따로_정한다() {
            검열_행(NicknameAuditStatus.CLEAN, "보임");
            검열_행(NicknameAuditStatus.PENDING, "가림");

            assertThat(exposureService.findRevealable(List.of("보임", "가림", "행없음")))
                    .containsExactly("보임");
        }
    }

    @Nested
    class 검열_행이_없는_닉네임 {

        @Test
        void 가린다() {
            assertThat(exposureService.findRevealable(List.of("새닉네임"))).isEmpty();
        }

        @Test
        void 검열_큐_등록을_요청한다() {
            검열_행(NicknameAuditStatus.CLEAN, "있는닉");

            exposureService.findRevealable(List.of("있는닉", "새닉네임"));

            then(eventPublisher).should().publishEvent(new UnregisteredNicknamesExposedEvent(Set.of("새닉네임")));
        }

        @Test
        void 모두_행이_있으면_등록을_요청하지_않는다() {
            검열_행(NicknameAuditStatus.PENDING, "있는닉");

            exposureService.findRevealable(List.of("있는닉"));

            then(eventPublisher).should(never()).publishEvent(any(Object.class));
        }

        @Test
        void 운영자가_허용한_단어와_같으면_공개하고_등록을_요청하지_않는다() {
            // register()가 운영자 허용 닉네임은 큐에 넣지 않는다. 가리면 판정받을 길이 없어 영영 풀리지 않는다.
            wordManagementService.operatorAllow("허용닉");

            assertThat(exposureService.findRevealable(List.of("허용닉"))).containsExactly("허용닉");
            then(eventPublisher).should(never()).publishEvent(any(Object.class));
        }
    }

    @Test
    void 빈_목록이나_null_닉네임은_공개하지_않는다() {
        assertThat(exposureService.findRevealable(List.of())).isEmpty();
        assertThat(exposureService.findRevealable(Arrays.asList(null, null))).isEmpty();
        then(eventPublisher).should(never()).publishEvent(any(Object.class));
    }

    private void 검열_행(NicknameAuditStatus status, String nickname) {
        final NicknameAudit audit = new NicknameAudit(nickname);
        audit.updateStatus(status);
        auditRepository.save(audit);
    }
}
