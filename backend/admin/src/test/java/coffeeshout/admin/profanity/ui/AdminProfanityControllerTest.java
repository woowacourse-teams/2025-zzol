package coffeeshout.admin.profanity.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import coffeeshout.admin.profanity.ui.request.AddProfanityWordRequest;
import coffeeshout.admin.profanity.ui.response.NicknameAuditResponse;
import coffeeshout.admin.profanity.ui.response.ProfanityWordResponse;
import coffeeshout.admin.support.PageResponse;
import coffeeshout.profanity.application.ProfanityAuditService;
import coffeeshout.profanity.application.ProfanityFeedbackService;
import coffeeshout.profanity.application.ProfanityWordManagementService;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.WordSource;
import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DisplayName("AdminProfanityController")
@ExtendWith(MockitoExtension.class)
class AdminProfanityControllerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-06T00:00:00Z"), KST);

    @Mock
    private ProfanityAuditService auditService;

    @Mock
    private ProfanityFeedbackService feedbackService;

    @Mock
    private ProfanityWordManagementService managementService;

    private AdminProfanityController controller() {
        return new AdminProfanityController(CLOCK, auditService, feedbackService, managementService);
    }

    @Nested
    class audits {

        @Test
        void 상태별로_조회하고_페이지_크기와_정렬을_고정한다() {
            given(auditService.listByStatus(any(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.<NicknameAudit>of()));

            controller().audits(NicknameAuditStatus.FLAGGED, 3);

            final PageRequest expected = PageRequest.of(
                    3, 10, org.springframework.data.domain.Sort.by("auditedAt").descending());
            then(auditService).should().listByStatus(NicknameAuditStatus.FLAGGED, expected);
        }

        @Test
        void 페이지_메타데이터를_함께_돌려준다() {
            given(auditService.listByStatus(any(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.<NicknameAudit>of(), PageRequest.of(0, 10), 25));

            final PageResponse<NicknameAuditResponse> response = controller().audits(NicknameAuditStatus.PENDING, 0);

            assertThat(response.totalElements()).isEqualTo(25);
            assertThat(response.totalPages()).isEqualTo(3);
        }
    }

    @Nested
    class 감사_조치 {

        @Test
        void 허용을_위임한다() {
            controller().allow(1L);

            then(feedbackService).should().allow(1L);
        }

        @Test
        void 차단을_위임한다() {
            controller().block(1L);

            then(feedbackService).should().block(1L);
        }
    }

    @Nested
    class words {

        @Test
        void 필터를_그대로_서비스에_넘긴다() {
            // 문자열을 직접 파싱하던 부분이 사라졌다. 값이 어긋나면 스프링이 400 을 낸다.
            given(managementService.findAllPaged("욕", Language.KOREAN, WordSource.MANUAL, true, 1, 20))
                    .willReturn(new PageImpl<>(List.of()));

            controller().words("욕", Language.KOREAN, WordSource.MANUAL, true, 1);

            then(managementService).should().findAllPaged("욕", Language.KOREAN, WordSource.MANUAL, true, 1, 20);
        }

        @Test
        void 필터가_없으면_null로_넘겨_전체를_조회한다() {
            given(managementService.findAllPaged("", null, null, null, 0, 20)).willReturn(new PageImpl<>(List.of()));

            controller().words("", null, null, null, 0);

            then(managementService).should().findAllPaged("", null, null, null, 0, 20);
        }

        @Test
        void 단어와_활성_여부를_돌려준다() {
            given(managementService.findAllPaged("", null, null, null, 0, 20))
                    .willReturn(
                            new PageImpl<>(List.of(new ProfanityWord("욕설", Language.KOREAN, WordSource.MANUAL, true))));

            assertThat(controller().words("", null, null, null, 0).content())
                    .containsExactly(new ProfanityWordResponse("욕설", Language.KOREAN, WordSource.MANUAL, true));
        }
    }

    @Nested
    class 단어_관리 {

        @Test
        void 수동_추가는_출처를_MANUAL로_고정한다() {
            controller().addWord(new AddProfanityWordRequest("욕설", Language.KOREAN));

            then(managementService).should().add("욕설", Language.KOREAN, WordSource.MANUAL);
        }

        @Test
        void 활성화를_위임한다() {
            controller().activate("욕설");

            then(managementService).should().activate("욕설");
        }

        @Test
        void 비활성화를_위임한다() {
            controller().deactivate("욕설");

            then(managementService).should().deactivate("욕설");
        }
    }

    @Nested
    class 응답_변환 {

        @Test
        void 신뢰도가_없으면_UNKNOWN으로_채운다() {
            // 화면이 null 을 따로 다루게 하면 표에 빈칸이 생긴다.
            final NicknameAudit audit = new NicknameAudit("닉네임");

            final NicknameAuditResponse response = NicknameAuditResponse.from(audit, KST);

            assertThat(response.confidence()).isEqualTo(AiConfidence.UNKNOWN);
            assertThat(response.reason()).isEmpty();
            assertThat(response.auditedAt()).isNull();
        }

        @Test
        void 검열이_끝났으면_신뢰도와_사유를_그대로_싣는다() {
            final NicknameAudit audit = new NicknameAudit("닉네임");
            audit.complete(NicknameAuditStatus.FLAGGED, AiConfidence.of(0.93), "욕설 포함");

            final NicknameAuditResponse response = NicknameAuditResponse.from(audit, KST);

            assertThat(response.status()).isEqualTo(NicknameAuditStatus.FLAGGED);
            assertThat(response.confidence()).isEqualTo(AiConfidence.of(0.93));
            assertThat(response.reason()).isEqualTo("욕설 포함");
            assertThat(response.auditedAt()).isNotNull();
        }
    }
}
