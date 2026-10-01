package coffeeshout.admin.inbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import coffeeshout.admin.inbox.domain.InboxItem;
import coffeeshout.admin.inbox.domain.InboxKind;
import coffeeshout.admin.system.application.SystemService;
import coffeeshout.admin.system.domain.DeadLetter;
import coffeeshout.admin.system.domain.DeadLetterSource;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.profanity.application.ProfanityAuditService;
import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.report.application.ReportAdminService;
import coffeeshout.report.application.ReportAdminService.ReportRow;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.domain.ReportStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("InboxService")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InboxServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-12T00:00:00Z"), KST);

    /** 기준 시각. 이 값에서 분을 빼며 "얼마나 오래됐는지"를 만든다. */
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 9, 12, 9, 0);

    @Mock
    private ReportAdminService reportAdminService;

    @Mock
    private ProfanityAuditService profanityAuditService;

    @Mock
    private SystemService systemService;

    private InboxService newService() {
        return new InboxService(reportAdminService, profanityAuditService, systemService, CLOCK);
    }

    @Nested
    class pending {

        @Test
        void 세_출처를_한_목록으로_합친다() {
            givenReports(report(1L, "앱이 종료됩니다", BASE.minusMinutes(10)));
            givenNicknames(NicknameAuditStatus.FLAGGED, audit(7L, "개새끼야", "직접적 욕설", BASE.minusMinutes(20)));
            givenNicknames(NicknameAuditStatus.PENDING);
            givenDeadLetters(
                    DeadLetterSource.OUTBOX, deadLetter(DeadLetterSource.OUTBOX, 3L, "room", BASE.minusMinutes(30)));
            givenDeadLetters(DeadLetterSource.SETTLEMENT);

            final List<InboxItem> items = newService().pending();

            assertThat(items)
                    .extracting(InboxItem::kind)
                    .containsExactly(InboxKind.REPORT, InboxKind.NICKNAME, InboxKind.DEAD_LETTER);
        }

        @Test
        void 최신순으로_정렬한다() {
            givenReports(report(1L, "오래된 신고", BASE.minusMinutes(90)));
            givenNicknames(NicknameAuditStatus.FLAGGED, audit(7L, "최근닉네임", "욕설", BASE.minusMinutes(5)));
            givenNicknames(NicknameAuditStatus.PENDING);
            givenDeadLetters(
                    DeadLetterSource.OUTBOX, deadLetter(DeadLetterSource.OUTBOX, 3L, "room", BASE.minusMinutes(40)));
            givenDeadLetters(DeadLetterSource.SETTLEMENT);

            final List<InboxItem> items = newService().pending();

            assertThat(items).extracting(InboxItem::title).containsExactly("최근닉네임", "room", "오래된 신고");
        }

        @Test
        void 스무_건까지만_내보낸다() {
            // 출처마다 20건씩 총 60건을 준다. 합쳐서 20건만 나와야 한다.
            givenReports(reports(20));
            givenNicknames(NicknameAuditStatus.FLAGGED, audits(20));
            givenNicknames(NicknameAuditStatus.PENDING);
            givenDeadLetters(DeadLetterSource.OUTBOX, deadLetters(20));
            givenDeadLetters(DeadLetterSource.SETTLEMENT);

            assertThat(newService().pending()).hasSize(20);
        }

        /**
         * 이 서비스가 기대는 불변식이다.
         *
         * <p>출처마다 최신 20건만 받아 오는데, 그것을 합쳐 자른 20건이 <b>전체에서 가장 새로운
         * 20건과 같아야</b> 한다. 한 출처가 스무 건을 훨씬 넘게 갖고 있어도, 그 출처의 21번째
         * 이후는 다른 출처의 최신 20건보다 오래됐으므로 어차피 목록에 들 수 없다.
         */
        @Test
        void 한_출처가_스무_건을_넘겨도_전체_최신순이_어긋나지_않는다() {
            // 신고는 1분 간격으로 촘촘하다. 조회가 최신 20건만 돌려주는 상황을 그대로 만든다.
            givenReports(reports(20));
            // 닉네임 하나가 그 무엇보다 새것이다. 신고가 아무리 많아도 이 건이 맨 위여야 한다.
            givenNicknames(NicknameAuditStatus.FLAGGED, audit(99L, "가장최근", "욕설", BASE));
            givenNicknames(NicknameAuditStatus.PENDING);
            givenDeadLetters(DeadLetterSource.OUTBOX);
            givenDeadLetters(DeadLetterSource.SETTLEMENT);

            final List<InboxItem> items = newService().pending();

            assertThat(items).hasSize(20);
            assertThat(items.get(0).title()).isEqualTo("가장최근");
            assertThat(items)
                    .isSortedAccordingTo((left, right) -> right.occurredAt().compareTo(left.occurredAt()));
        }

        @Test
        void 처리할_것이_없으면_빈_목록이다() {
            givenReports();
            givenNicknames(NicknameAuditStatus.FLAGGED);
            givenNicknames(NicknameAuditStatus.PENDING);
            givenDeadLetters(DeadLetterSource.OUTBOX);
            givenDeadLetters(DeadLetterSource.SETTLEMENT);

            assertThat(newService().pending()).isEmpty();
        }

        @Test
        void 격리_메시지_식별자에_출처를_붙인다() {
            // outbox 와 settlement 의 id 가 겹치므로 숫자만으로는 무엇을 폐기할지 정해지지 않는다.
            givenReports();
            givenNicknames(NicknameAuditStatus.FLAGGED);
            givenNicknames(NicknameAuditStatus.PENDING);
            givenDeadLetters(
                    DeadLetterSource.OUTBOX, deadLetter(DeadLetterSource.OUTBOX, 5L, "room", BASE.minusMinutes(1)));
            givenDeadLetters(
                    DeadLetterSource.SETTLEMENT,
                    deadLetter(DeadLetterSource.SETTLEMENT, 5L, "settlement:result", BASE.minusMinutes(2)));

            assertThat(newService().pending()).extracting(InboxItem::id).containsExactly("OUTBOX:5", "SETTLEMENT:5");
        }

        @Test
        void 신고의_LocalDateTime_을_시간대에_맞춰_변환한다() {
            // 신고만 LocalDateTime 이고 나머지는 Instant 다. 변환이 빠지면 아홉 시간이 어긋나
            // 신고가 늘 목록 맨 위나 맨 아래에 몰린다.
            givenReports(report(1L, "신고", BASE));
            givenNicknames(NicknameAuditStatus.FLAGGED);
            givenNicknames(NicknameAuditStatus.PENDING);
            givenDeadLetters(DeadLetterSource.OUTBOX);
            givenDeadLetters(DeadLetterSource.SETTLEMENT);

            assertThat(newService().pending().get(0).occurredAt())
                    .isEqualTo(BASE.atZone(KST).toInstant());
        }
    }

    private void givenReports(ReportRow... rows) {
        givenReports(List.of(rows));
    }

    private void givenReports(List<ReportRow> rows) {
        given(reportAdminService.list(eq(ReportStatus.PENDING), eq(null), eq(null), eq(0)))
                .willReturn(new PageImpl<>(rows, PageRequest.of(0, 20), rows.size()));
    }

    private void givenNicknames(NicknameAuditStatus status, NicknameAudit... audits) {
        givenNicknames(status, List.of(audits));
    }

    private void givenNicknames(NicknameAuditStatus status, List<NicknameAudit> audits) {
        final Page<NicknameAudit> page = new PageImpl<>(audits, PageRequest.of(0, 20), audits.size());
        given(profanityAuditService.listByStatus(eq(status), any())).willReturn(page);
    }

    private void givenDeadLetters(DeadLetterSource source, DeadLetter... letters) {
        givenDeadLetters(source, List.of(letters));
    }

    private void givenDeadLetters(DeadLetterSource source, List<DeadLetter> letters) {
        given(systemService.findDeadLetters(eq(source), eq(0), anyInt()))
                .willReturn(new PageImpl<>(letters, PageRequest.of(0, 20), letters.size()));
    }

    private static List<ReportRow> reports(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> report(index + 1L, "신고 " + index, BASE.minusMinutes(index + 1L)))
                .toList();
    }

    private static List<NicknameAudit> audits(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> audit(index + 1L, "닉네임" + index, "욕설", BASE.minusMinutes(index + 1L)))
                .toList();
    }

    private static List<DeadLetter> deadLetters(int count) {
        return IntStream.range(0, count)
                .mapToObj(
                        index -> deadLetter(DeadLetterSource.OUTBOX, index + 1L, "room", BASE.minusMinutes(index + 1L)))
                .toList();
    }

    private static ReportRow report(Long id, String content, LocalDateTime createdAt) {
        return new ReportRow(
                id,
                ReportCategory.BUG,
                MiniGameType.CARD_GAME,
                "ABC12",
                content,
                ReportStatus.PENDING,
                createdAt,
                null,
                null);
    }

    /**
     * 감사 엔티티는 생성자가 {@code createdAt} 을 스스로 박는다. 시더와 같은 이유로 도메인에
     * 시각 주입 생성자를 열지 않으므로, 테스트에서는 필드를 직접 넣어 기준 시각을 만든다.
     */
    private static NicknameAudit audit(Long id, String nickname, String reason, LocalDateTime createdAt) {
        final NicknameAudit audit = new NicknameAudit(nickname);
        audit.complete(NicknameAuditStatus.FLAGGED, AiConfidence.of(0.9), reason);
        ReflectionTestUtils.setField(audit, "id", id);
        ReflectionTestUtils.setField(audit, "createdAt", createdAt.atZone(KST).toInstant());
        return audit;
    }

    private static DeadLetter deadLetter(DeadLetterSource source, Long id, String reference, LocalDateTime createdAt) {
        return new DeadLetter(
                source,
                id,
                reference,
                "발행 재시도 10회 소진",
                "{}",
                10,
                createdAt.atZone(KST).toInstant());
    }
}
