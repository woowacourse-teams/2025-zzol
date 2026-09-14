package coffeeshout.report.config;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.infra.persistence.Report;
import coffeeshout.report.infra.persistence.ReportRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로컬 개발 환경에서 페이지 디자인 확인용 mock 신고 데이터를 생성합니다. PAGE_SIZE=20 기준 3페이지(총 43건)를 삽입합니다.
 */
@Slf4j
@Profile("local")
@Component
@RequiredArgsConstructor
public class LocalReportDataInitializer implements ApplicationRunner {

    /**
     * 처리 소요 시간 후보(분).
     *
     * <p>대부분은 몇 시간 안에 처리되고 가끔 며칠 걸린다. 전부 비슷한 값으로 두면 처리
     * 시간 분포가 막대 하나가 되고, p50 과 p95 를 나눠 둔 이유도 화면에서 안 보인다.
     */
    private static final List<Integer> RESOLVE_MINUTES =
            List.of(25, 40, 95, 150, 280, 420, 610, 900, 1_500, 2_600, 5_000);

    /** 고정 씨앗. 기동할 때마다 같은 데이터가 나와야 화면 변경을 비교할 수 있다. */
    private static final long SEED = 20_260_912L;

    /** 지난 신고에 붙일 게임. 앞쪽이 신고가 잦은 게임이다. */
    private static final List<MiniGameType> GAME_POOL = List.of(
            MiniGameType.CARD_GAME,
            MiniGameType.CARD_GAME,
            MiniGameType.RACING_GAME,
            MiniGameType.SPEED_TOUCH,
            MiniGameType.BLOCK_STACKING);

    private final ReportRepository reportRepository;
    private final Clock clock;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (reportRepository.count() > 0) {
            log.debug("[LocalReportDataInitializer] 이미 데이터가 존재하므로 건너뜁니다.");
            return;
        }

        final List<Report> entities = buildMockData();
        reportRepository.saveAll(entities);
        backdateResolvedAt(entities);
        log.info("[LocalReportDataInitializer] mock 신고 데이터 {}건 삽입 완료", entities.size());
    }

    // PMD.NcssCount 억제 — 이 메서드는 로직이 아니라 mock 데이터 표다(list.add 나열).
    // 카테고리별로 쪼개도 표가 여러 조각이 될 뿐 추상화 수준이 나뉘지 않는다.
    @SuppressWarnings("PMD.NcssCount")
    private List<Report> buildMockData() {
        final List<Report> list = new ArrayList<>();
        final Instant base = Instant.now(clock);

        // 1. BUG — 미니게임 버그 (13건)
        // 길이를 일부러 한계(200자)까지 늘린 건이다. 짧은 문장만 있으면 내용 열이 늘 한 줄에
        // 들어가서 어느 폭이 맞는지를 화면에서 정할 수 없다. 실제 신고는 이만큼 길게 들어온다.
        list.add(bug(
                MiniGameType.CARD_GAME,
                "ABC12",
                "카드게임 시작 후 5초 만에 앱이 강제 종료됩니다. 안드로이드 14, 갤럭시 S23 울트라에서 와이파이와 LTE 둘 다 같은 증상이고 방을 새로 만들어도 똑같습니다. "
                        + "다른 미니게임은 멀쩡한데 카드게임만 그렇고 오늘만 네 번 겪었습니다. 로그가 필요하면 보내드릴게요.",
                base,
                0));
        list.add(bug(
                MiniGameType.CARD_GAME, "XYZ99", "상대방 카드가 화면에 표시되지 않아요. 제 카드는 보이는데 다른 사람 자리는 뒷면만 계속 떠 있습니다.", base, 1));
        list.add(bug(MiniGameType.RACING_GAME, "QWE34", "레이싱 게임 결과 화면에서 점수가 0으로 나옵니다.", base, 2));
        list.add(bug(MiniGameType.SPEED_TOUCH, "RTY56", "스피드 터치 버튼이 가끔 반응하지 않습니다.", base, 3));
        list.add(bug(MiniGameType.BLIND_TIMER, "UIO78", "블라인드 타이머가 0초에서 멈추지 않아요.", base, 4));
        list.add(bug(MiniGameType.BLOCK_STACKING, "DFG11", "블록 쌓기 중 화면이 갑자기 하얗게 변합니다.", base, 5));
        list.add(bug(MiniGameType.CARD_GAME, "HJK22", "카드 뒤집기 애니메이션이 재생되지 않습니다.", base, 6));
        list.add(bug(MiniGameType.RACING_GAME, "LZX33", "레이싱 게임 BGM이 게임 종료 후에도 계속 재생됩니다.", base, 7));
        list.add(bug(MiniGameType.SPEED_TOUCH, "CVB44", "스피드 터치 시작 카운트다운이 보이지 않아요.", base, 8));
        list.add(bug(MiniGameType.CARD_GAME, "WER66", "카드게임 방 입장 시 무한 로딩이 발생합니다.", base, 9));
        list.add(bug(MiniGameType.BLOCK_STACKING, "TYU77", "블록 쌓기 최고 점수가 갱신되지 않습니다.", base, 12));
        list.add(bug(MiniGameType.BLIND_TIMER, "IOP88", "블라인드 타이머 힌트 기능이 동작하지 않아요.", base, 13));
        list.add(bug(MiniGameType.RACING_GAME, "ASD01", "레이싱 게임 조이스틱 입력이 씹힙니다.", base, 14));

        // 2. SUGGESTION — 건의사항 (15건)
        list.add(suggestion(
                "다크모드 지원을 추가해주세요. 밤에 친구들과 방을 잡고 노는 일이 많은데 흰 화면이 너무 밝습니다. "
                        + "시스템 설정을 따라가는 자동 전환이면 가장 좋고 안 되면 설정에 토글 하나만 있어도 충분합니다. 룰렛 화면이 특히 눈이 부십니다.",
                base,
                15));
        list.add(suggestion("게임 결과 공유 기능이 있으면 좋겠어요.", base, 16));
        list.add(suggestion("방 비밀번호 설정 기능을 추가해주세요.", base, 17));
        list.add(suggestion("친구 목록 기능을 만들어주세요.", base, 18));
        list.add(suggestion("게임 내 채팅 기능을 넣어주세요.", base, 19));
        list.add(suggestion("마이페이지에서 플레이 전적을 볼 수 있으면 좋겠어요.", base, 20));
        list.add(suggestion("방 목록 검색 기능이 필요합니다.", base, 21));
        list.add(suggestion("닉네임 변경 주기를 늘려주세요.", base, 22));
        list.add(suggestion("게임별 랭킹 보드를 추가해주세요.", base, 23));
        list.add(suggestion("iOS 앱 출시 예정이 있나요?", base, 24));
        list.add(suggestion("방장 위임 기능을 추가해주세요.", base, 25));
        list.add(suggestion("게임 중 이모지 반응 기능이 있으면 재미있을 것 같아요.", base, 26));
        list.add(suggestion("새 게임 추가 요청: 스무고개 게임이요!", base, 27));
        list.add(suggestion("카드게임 덱 커스터마이징 기능을 지원해주세요.", base, 28));
        list.add(suggestion("게임 결과 스크린샷 저장 버튼을 만들어주세요.", base, 29));

        // 3. GAME_REQUEST / OTHER 혼합 (15건, 일부 RESOLVED)
        list.add(gameRequest("끝말잇기 게임을 추가해주세요.", base, 30));
        list.add(gameRequest("초성 퀴즈 게임이 있으면 좋겠어요.", base, 31));
        list.add(gameRequest("369 게임 넣어주세요!", base, 32));
        list.add(gameRequest("사다리타기 게임을 추가해주세요.", base, 33));
        list.add(gameRequest("스피드 퀴즈 기능이 필요합니다.", base, 34));

        Report resolved1 = bug(MiniGameType.CARD_GAME, "RSV01", "(처리 완료) 카드게임 로딩 버그 — 패치됨.", base, 35);
        resolved1.resolve();
        list.add(resolved1);

        Report resolved2 = suggestion("(처리 완료) 다크모드 — 다음 업데이트에 반영 예정.", base, 36);
        resolved2.resolve();
        list.add(resolved2);

        Report resolved3 = gameRequest("(처리 완료) 블록 쌓기 게임 추가 완료.", base, 37);
        resolved3.resolve();
        list.add(resolved3);

        list.add(other("운영자 연락처를 알고 싶어요.", base, 38));
        list.add(other("커피빵 굿즈를 팔면 살게요!", base, 39));
        list.add(other(
                "서비스 이용약관 링크가 깨져 있습니다. 마이페이지 아래쪽에서 누르면 404가 뜨고 회원가입 화면의 링크도 같은 주소라 똑같이 안 열립니다. 모바일 사파리와 크롬 둘 다 확인했습니다.",
                base,
                40));
        list.add(other("광고 문의는 어디로 하나요?", base, 41));
        list.add(other("피드백 감사합니다. 계속 발전해주세요!", base, 42));

        Report resolved4 = other("(처리 완료) 이용약관 링크 수정 완료.", base, 43);
        resolved4.resolve();
        list.add(resolved4);

        list.add(other("서버가 불안정한 것 같아요. 확인 부탁드립니다.", base, 44));

        list.addAll(settledHistory(base));
        return list;
    }

    /**
     * 이미 처리가 끝난 지난 신고.
     *
     * <p>처리 완료가 네 건뿐이라 처리 시간 분포가 칸마다 막대 하나였고 p95 가 표본 넷에서
     * 나왔다. 실제로는 미처리보다 처리된 것이 훨씬 많이 쌓여 있다. 문장을 손으로 적지 않고
     * 돌려 쓰는 이유는, 이 건들은 <b>목록에서 읽히려고</b> 있는 것이 아니라 분포를 만들려고
     * 있어서다. 화면에서 문장 길이를 시험하는 일은 위의 손으로 적은 건들이 맡는다.
     */
    private List<Report> settledHistory(Instant base) {
        final List<String> topics =
                List.of("게임 중 튕김 현상", "결과 화면 점수 표시", "룰렛 애니메이션", "방 입장 지연", "소리가 안 나는 문제", "닉네임 변경 오류");

        final List<Report> settled = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            final String content = "(처리 완료) " + topics.get(i % topics.size()) + " 문의 - 확인 후 조치했습니다.";
            final Report report = i % 3 == 0
                    ? bug(GAME_POOL.get(i % GAME_POOL.size()), "OLD" + (10 + i), content, base, 8 + i * 2)
                    : suggestion(content, base, 8 + i * 2);
            report.resolve();
            settled.add(report);
        }
        return settled;
    }

    /**
     * 처리 시각을 접수 이후로 되돌린다.
     *
     * <p>{@code Report.resolve()} 가 {@code Instant.now()} 를 박는다. 그대로 두면 한 달 전에
     * 접수된 신고가 방금 처리된 것이 되어, 처리 소요 시간 분포가 전부 "3일 초과" 한 칸에
     * 몰린다. 도메인에 시각 주입 생성자를 열지 않고 여기서 되돌린다. 그 구멍은 운영
     * 코드에서도 쓸 수 있게 되고, 그때부터 이 필드는 "기록된 시각"이 아니라 "누군가 정한
     * 시각"이 된다.
     *
     * <p>JdbcTemplate 이 아니라 JPQL 이다. 드라이버 변환 경로가 삽입 때와 달라 시간대가
     * 어긋난 적이 있다(닉네임 검열 시더에 같은 주석이 있다).
     */
    private void backdateResolvedAt(List<Report> reports) {
        final Random random = new Random(SEED);
        for (Report report : reports) {
            if (report.getResolvedAt() == null) {
                continue;
            }
            final Instant resolvedAt = report.getCreatedAt()
                    .plus(RESOLVE_MINUTES.get(random.nextInt(RESOLVE_MINUTES.size())), ChronoUnit.MINUTES);
            entityManager
                    .createQuery("UPDATE Report r SET r.resolvedAt = :at WHERE r.id = :id")
                    .setParameter("at", resolvedAt)
                    .setParameter("id", report.getId())
                    .executeUpdate();
        }
    }

    /**
     * 손으로 적어 둔 순서를 한 달에 흩는다.
     *
     * <p>원래는 1~44시간이라 마흔네 건이 전부 이틀 안에 들어왔다. 신고 화면의 일자별
     * 그래프가 오른쪽 끝에 막대 두 개로 서고 나머지 28일이 통째로 빈칸이었다. 처음 여섯
     * 건은 시각을 그대로 둔다. "방금", "3시간 전" 같은 최근 표기가 화면에 남아야
     * 상대시각 표시가 제대로 그려지는지 확인할 수 있다.
     */
    private static Instant occurredAt(Instant base, int offsetHours) {
        return base.minus(offsetHours <= 6 ? offsetHours : offsetHours * 16L, ChronoUnit.HOURS);
    }

    private Report bug(MiniGameType gameType, String joinCode, String content, Instant base, int offsetHours) {
        return Report.createBugReport(gameType, joinCode, content, occurredAt(base, offsetHours));
    }

    private Report suggestion(String content, Instant base, int offsetHours) {
        return Report.createGeneralReport(ReportCategory.SUGGESTION, content, occurredAt(base, offsetHours));
    }

    private Report gameRequest(String content, Instant base, int offsetHours) {
        return Report.createGeneralReport(ReportCategory.GAME_REQUEST, content, occurredAt(base, offsetHours));
    }

    private Report other(String content, Instant base, int offsetHours) {
        return Report.createGeneralReport(ReportCategory.OTHER, content, occurredAt(base, offsetHours));
    }
}
