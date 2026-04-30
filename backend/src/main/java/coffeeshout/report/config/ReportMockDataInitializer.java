package coffeeshout.report.config;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.report.domain.ReportCategory;
import coffeeshout.report.infra.persistence.Report;
import coffeeshout.report.infra.persistence.ReportRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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
public class ReportMockDataInitializer implements ApplicationRunner {

    private final ReportRepository reportRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (reportRepository.count() > 0) {
            log.debug("[ReportMockDataInitializer] 이미 데이터가 존재하므로 건너뜁니다.");
            return;
        }

        final List<Report> entities = buildMockData();
        reportRepository.saveAll(entities);
        log.info("[ReportMockDataInitializer] mock 신고 데이터 {}건 삽입 완료", entities.size());
    }

    private List<Report> buildMockData() {
        final List<Report> list = new ArrayList<>();
        final Instant base = Instant.now(clock);

        // 1. BUG — 미니게임 버그 (13건)
        list.add(bug(MiniGameType.CARD_GAME, "ABC12", "카드게임 시작 후 5초 만에 앱이 강제 종료됩니다.", base, 0));
        list.add(bug(MiniGameType.CARD_GAME, "XYZ99", "상대방 카드가 화면에 표시되지 않아요.", base, 1));
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
        list.add(suggestion("다크모드 지원을 추가해주세요.", base, 15));
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
        list.add(other("서비스 이용약관 링크가 깨져 있습니다.", base, 40));
        list.add(other("광고 문의는 어디로 하나요?", base, 41));
        list.add(other("피드백 감사합니다. 계속 발전해주세요!", base, 42));

        Report resolved4 = other("(처리 완료) 이용약관 링크 수정 완료.", base, 43);
        resolved4.resolve();
        list.add(resolved4);

        list.add(other("서버가 불안정한 것 같아요. 확인 부탁드립니다.", base, 44));

        return list;
    }

    private Report bug(MiniGameType gameType, String joinCode, String content, Instant base, int offsetHours) {
        return Report.createBugReport(gameType, joinCode, content, base.minus(offsetHours, ChronoUnit.HOURS));
    }

    private Report suggestion(String content, Instant base, int offsetHours) {
        return Report.createGeneralReport(ReportCategory.SUGGESTION, content,
                base.minus(offsetHours, ChronoUnit.HOURS));
    }

    private Report gameRequest(String content, Instant base, int offsetHours) {
        return Report.createGeneralReport(ReportCategory.GAME_REQUEST, content,
                base.minus(offsetHours, ChronoUnit.HOURS));
    }

    private Report other(String content, Instant base, int offsetHours) {
        return Report.createGeneralReport(ReportCategory.OTHER, content, base.minus(offsetHours, ChronoUnit.HOURS));
    }
}
