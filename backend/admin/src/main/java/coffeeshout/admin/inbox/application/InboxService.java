package coffeeshout.admin.inbox.application;

import coffeeshout.admin.inbox.domain.InboxItem;
import coffeeshout.admin.inbox.domain.InboxKind;
import coffeeshout.admin.system.application.SystemService;
import coffeeshout.admin.system.domain.DeadLetter;
import coffeeshout.admin.system.domain.DeadLetterSource;
import coffeeshout.profanity.application.ProfanityAuditService;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.report.application.ReportAdminService;
import coffeeshout.report.application.ReportAdminService.ReportRow;
import coffeeshout.report.domain.ReportStatus;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 작업함.
 *
 * <p>처리할 일이 신고, 닉네임 검열, 격리 메시지 세 화면에 흩어져 있었다. 홈은 건수만
 * 보여주고 각 화면으로 보냈으므로, 오늘 할 일을 끝내려면 세 화면을 왕복해야 했고 화면마다
 * 표의 모양이 달랐다. <b>여기서 한 번에 훑고 그 자리에서 처리한다.</b>
 *
 * <p>집계는 하지 않는다. 건수는 이미 {@code OverviewService.actionQueue()}가 센다. 두 곳이
 * 같은 것을 따로 세면 언젠가 다른 숫자를 말하게 된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InboxService {

    /**
     * 출처마다 가져오는 건수이자 합쳐서 내보내는 건수다.
     *
     * <p>둘을 같은 값으로 두는 것이 정확성의 조건이다. 각 출처에서 <b>최신 20건</b>을 받아
     * 합치면 전체에서 최신 20건이 반드시 그 안에 있다. 출처마다 20건을 받아 40건을
     * 내보내려 하면, 어떤 출처에 21번째로 새 것이 있어도 못 본 채로 목록이 만들어진다.
     *
     * <p>신고 조회의 페이지 크기가 20으로 고정돼 있어 그 값에 맞췄다. 더 받으려면 페이지를
     * 여러 번 넘겨야 하는데, 작업함은 전부를 보는 화면이 아니라 <b>다음에 처리할 것</b>을
     * 보는 화면이라 그럴 이유가 없다. 전체 목록은 각 화면에 있다.
     */
    private static final int LIMIT = 20;

    private final ReportAdminService reportAdminService;
    private final ProfanityAuditService profanityAuditService;
    private final SystemService systemService;
    private final Clock clock;

    /**
     * 최신순으로 합친다.
     *
     * <p>오래 방치된 것을 위로 올리는 편이 작업함답지만 그렇게 하지 않았다. 신고와 격리
     * 메시지의 조회가 최신순으로 <b>고정</b>돼 있어, 오래된 순으로 뒤집으려면 두 모듈의
     * 정렬을 바꾸거나 전부 읽어 와야 한다. 일부만 읽고 뒤집으면 "최신 20건 중 가장 오래된
     * 것"이 나오는데, 그건 틀린 목록이면서 틀린 티가 안 난다. <b>정확한 최신순이 부정확한
     * 오래된순보다 낫다.</b>
     *
     * <p>오래 방치된 건은 신고 화면의 "가장 오래 기다린 건"이 따로 말한다.
     */
    public List<InboxItem> pending() {
        return Stream.of(reports(), nicknames(), deadLetters())
                .flatMap(List::stream)
                .sorted(Comparator.comparing(InboxItem::occurredAt).reversed())
                .limit(LIMIT)
                .toList();
    }

    private List<InboxItem> reports() {
        return reportAdminService.list(ReportStatus.PENDING, null, null, 0).getContent().stream()
                .map(this::toItem)
                .toList();
    }

    private InboxItem toItem(ReportRow row) {
        return new InboxItem(
                InboxKind.REPORT,
                String.valueOf(row.id()),
                row.content(),
                row.category().name(),
                row.createdAt().atZone(clock.getZone()).toInstant());
    }

    /**
     * 걸러낸 것과 판단 못한 것을 함께 담는다.
     *
     * <p>둘은 다른 상태지만 운영자가 할 일은 같다. 읽고 허용하거나 차단한다. 목록에서
     * 나누면 같은 동작을 두 번에 걸쳐 하게 된다. 어느 쪽인지는 사유가 말한다.
     */
    private List<InboxItem> nicknames() {
        final PageRequest newestFirst =
                PageRequest.of(0, LIMIT, Sort.by("createdAt").descending());
        return Stream.of(NicknameAuditStatus.FLAGGED, NicknameAuditStatus.PENDING)
                .flatMap(status -> profanityAuditService.listByStatus(status, newestFirst).getContent().stream())
                .map(this::toItem)
                .toList();
    }

    private InboxItem toItem(NicknameAudit audit) {
        return new InboxItem(
                InboxKind.NICKNAME,
                String.valueOf(audit.getId()),
                audit.getNickname(),
                audit.getReason(),
                audit.getCreatedAt());
    }

    /**
     * 두 출처를 합친다. 발행 실패와 정산 격리는 원인이 다르지만 둘 다 "멈춰 있는 메시지"다.
     *
     * <p>식별자에 출처를 붙인다. 두 테이블의 id 가 겹치므로 숫자만으로는 무엇을 폐기해야
     * 하는지 정해지지 않는다.
     */
    private List<InboxItem> deadLetters() {
        return Stream.of(DeadLetterSource.values())
                .flatMap(source -> systemService.findDeadLetters(source, 0, LIMIT).getContent().stream())
                .map(this::toItem)
                .toList();
    }

    private InboxItem toItem(DeadLetter deadLetter) {
        return new InboxItem(
                InboxKind.DEAD_LETTER,
                deadLetter.source().name() + ":" + deadLetter.id(),
                deadLetter.reference(),
                deadLetter.reason(),
                deadLetter.createdAt());
    }
}
