package coffeeshout.admin.overview.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import coffeeshout.admin.ipblock.IpBlockAdminService;
import coffeeshout.admin.overview.application.OverviewService.ActionQueue;
import coffeeshout.admin.overview.application.OverviewService.DailySummary;
import coffeeshout.admin.overview.application.OverviewService.PeriodSummary;
import coffeeshout.admin.overview.domain.OverviewStatisticsRepository;
import coffeeshout.admin.overview.domain.RoomFunnel;
import coffeeshout.admin.system.application.SystemService;
import coffeeshout.global.ipblock.IpBlockStore.BlockedIp;
import coffeeshout.profanity.application.ProfanityAuditService;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.report.application.ReportAdminService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DisplayName("OverviewService")
@ExtendWith(MockitoExtension.class)
class OverviewServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    // KST 2026-09-06 09:00
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-06T00:00:00Z"), KST);

    @Mock
    private OverviewStatisticsRepository overviewStatisticsRepository;

    @Mock
    private ReportAdminService reportAdminService;

    @Mock
    private ProfanityAuditService profanityAuditService;

    @Mock
    private IpBlockAdminService ipBlockAdminService;

    @Mock
    private SystemService systemService;

    private OverviewService service() {
        return new OverviewService(
                overviewStatisticsRepository,
                reportAdminService,
                profanityAuditService,
                ipBlockAdminService,
                systemService,
                CLOCK);
    }

    private void givenAuditCount(NicknameAuditStatus status, long total) {
        given(profanityAuditService.listByStatus(eq(status), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.<NicknameAudit>of(), PageRequest.of(0, 1), total));
    }

    @Nested
    class actionQueue {

        @Test
        void 네_가지_대기_건수를_모은다() {
            given(reportAdminService.countPending()).willReturn(3L);
            givenAuditCount(NicknameAuditStatus.FLAGGED, 5L);
            givenAuditCount(NicknameAuditStatus.PENDING, 2L);
            given(ipBlockAdminService.getBlockedIps()).willReturn(List.of(new BlockedIp("1.2.3.4", 60)));

            final ActionQueue queue = service().actionQueue();

            assertThat(queue.pendingReports()).isEqualTo(3L);
            assertThat(queue.flaggedNicknames()).isEqualTo(5L);
            assertThat(queue.pendingNicknames()).isEqualTo(2L);
            assertThat(queue.blockedIps()).isEqualTo(1);
        }

        @Test
        void 하나라도_있으면_할_일이_있다() {
            given(reportAdminService.countPending()).willReturn(0L);
            givenAuditCount(NicknameAuditStatus.FLAGGED, 0L);
            givenAuditCount(NicknameAuditStatus.PENDING, 1L);
            given(ipBlockAdminService.getBlockedIps()).willReturn(List.of());

            assertThat(service().actionQueue().hasWork()).isTrue();
        }

        @Test
        void 전부_0이면_할_일이_없다() {
            given(reportAdminService.countPending()).willReturn(0L);
            givenAuditCount(NicknameAuditStatus.FLAGGED, 0L);
            givenAuditCount(NicknameAuditStatus.PENDING, 0L);
            given(ipBlockAdminService.getBlockedIps()).willReturn(List.of());

            assertThat(service().actionQueue().hasWork()).isFalse();
        }

        @Test
        void 개수만_필요하므로_한_건만_조회한다() {
            given(reportAdminService.countPending()).willReturn(0L);
            givenAuditCount(NicknameAuditStatus.FLAGGED, 0L);
            givenAuditCount(NicknameAuditStatus.PENDING, 0L);
            given(ipBlockAdminService.getBlockedIps()).willReturn(List.of());

            service().actionQueue();

            final ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            then(profanityAuditService).should(times(2)).listByStatus(any(), pageable.capture());
            assertThat(pageable.getAllValues())
                    .allSatisfy(page -> assertThat(page.getPageSize()).isEqualTo(1));
        }
    }

    @Nested
    class summaryOf {

        @Test
        void 하루_경계를_상한_배타로_잡는다() {
            // 자정 정각에 생긴 방이 이틀에 걸쳐 두 번 세지면 안 된다.
            given(overviewStatisticsRepository.findFunnelBetween(any(), any())).willReturn(RoomFunnel.empty());

            service().summaryOf(LocalDate.of(2026, 9, 6));

            final ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
            final ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
            then(overviewStatisticsRepository).should().findFunnelBetween(from.capture(), to.capture());
            assertThat(from.getValue()).isEqualTo(LocalDateTime.of(2026, 9, 6, 0, 0));
            assertThat(to.getValue()).isEqualTo(LocalDateTime.of(2026, 9, 7, 0, 0));
        }

        @Test
        void 가입은_같은_경계를_Instant로_변환해_조회한다() {
            // app_user.created_at 만 Instant 컬럼이라 시간대 변환이 필요하다.
            given(overviewStatisticsRepository.findFunnelBetween(any(), any())).willReturn(RoomFunnel.empty());

            service().summaryOf(LocalDate.of(2026, 9, 6));

            final ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
            final ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
            then(overviewStatisticsRepository).should().countSignupsBetween(from.capture(), to.capture());
            assertThat(from.getValue()).isEqualTo(Instant.parse("2026-09-05T15:00:00Z"));
            assertThat(to.getValue()).isEqualTo(Instant.parse("2026-09-06T15:00:00Z"));
        }

        @Test
        void 퍼널과_참여자와_가입_수를_함께_돌려준다() {
            given(overviewStatisticsRepository.findFunnelBetween(any(), any()))
                    .willReturn(new RoomFunnel(10, 8, 6, 4, 3));
            given(overviewStatisticsRepository.countPlayersBetween(any(), any()))
                    .willReturn(42L);
            given(overviewStatisticsRepository.countSignupsBetween(any(), any()))
                    .willReturn(7L);

            final DailySummary summary = service().summaryOf(LocalDate.of(2026, 9, 6));

            assertThat(summary.funnel().completed()).isEqualTo(3L);
            assertThat(summary.players()).isEqualTo(42L);
            assertThat(summary.signups()).isEqualTo(7L);
        }
    }

    @Nested
    class periodSummary {

        @Test
        void 구간을_한_번에_집계한다() {
            // 하루치를 N번 더하지 않는다. 어제 생기고 오늘 끝난 방이 두 날로 갈리면
            // 어느 날짜에서도 퍼널이 맞지 않는다.
            given(overviewStatisticsRepository.findFunnelBetween(any(), any())).willReturn(RoomFunnel.empty());

            service().periodSummary(7);

            final ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
            final ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
            then(overviewStatisticsRepository).should().findFunnelBetween(from.capture(), to.capture());
            // 오늘(09-06)을 포함한 7일이므로 09-31 이 아니라 08-31 부터다.
            assertThat(from.getValue()).isEqualTo(LocalDateTime.of(2026, 8, 31, 0, 0));
            assertThat(to.getValue()).isEqualTo(LocalDateTime.of(2026, 9, 7, 0, 0));
        }

        @Test
        void 구간의_시작과_끝_날짜를_함께_돌려준다() {
            given(overviewStatisticsRepository.findFunnelBetween(any(), any())).willReturn(RoomFunnel.empty());

            final PeriodSummary summary = service().periodSummary(30);

            assertThat(summary.days()).isEqualTo(30);
            assertThat(summary.from()).isEqualTo(LocalDate.of(2026, 8, 8));
            assertThat(summary.to()).isEqualTo(LocalDate.of(2026, 9, 6));
        }

        @Test
        void 방당_평균_참여자는_생성된_방_전체로_나눈다() {
            // 사람이 모인 방만으로 나누면 값이 늘 3~4 에 고정돼
            // 사람이 안 모이고 있다는 사실이 지표에서 사라진다.
            given(overviewStatisticsRepository.findFunnelBetween(any(), any()))
                    .willReturn(new RoomFunnel(10, 4, 4, 3, 3));
            given(overviewStatisticsRepository.countPlayersBetween(any(), any()))
                    .willReturn(20L);

            assertThat(service().periodSummary(30).avgPlayersPerRoom()).isEqualTo(2.0);
        }

        @Test
        void 방이_하나도_없으면_평균은_0이다() {
            given(overviewStatisticsRepository.findFunnelBetween(any(), any())).willReturn(RoomFunnel.empty());
            given(overviewStatisticsRepository.countPlayersBetween(any(), any()))
                    .willReturn(0L);

            assertThat(service().periodSummary(30).avgPlayersPerRoom()).isZero();
        }
    }

    @Nested
    class today {

        @Test
        void 시계의_시간대_기준_오늘을_쓴다() {
            // UTC 2026-09-06T00:00Z 는 KST 로 09-06 09:00 이다.
            given(overviewStatisticsRepository.findFunnelBetween(any(), any())).willReturn(RoomFunnel.empty());

            assertThat(service().today().date()).isEqualTo(LocalDate.of(2026, 9, 6));
        }
    }
}
