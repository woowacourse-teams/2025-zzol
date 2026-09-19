package coffeeshout.profanity.infra;

import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalNicknameAuditDataInitializer implements ApplicationRunner {

    /** 14일에 걸쳐 흩는다. 검열 대기는 신고보다 빨리 쌓이고 빨리 처리되는 편이다. */
    private static final int DAYS = 14;

    /** 고정 씨앗. 기동할 때마다 같은 데이터가 나와야 화면 변경을 비교할 수 있다. */
    private static final long SEED = 20_260_912L;

    private final NicknameAuditRepository auditRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (auditRepository.countByStatus(NicknameAuditStatus.FLAGGED) > 0
                || auditRepository.countByStatus(NicknameAuditStatus.PENDING) > 0) {
            log.info("[LocalInit] 닉네임 검열 데이터가 이미 존재합니다. 초기 데이터 삽입을 건너뜁니다.");
            return;
        }

        final List<NicknameAudit> flaggedData = List.of(
                flagged("씨b알", 0.97, "비속어 우회 (특수문자 삽입)"),
                flagged("ㅅㅂ놈아", 0.95, "초성 비속어"),
                flagged("개새끼야", 0.99, "직접적 욕설"),
                flagged("존나빠름", 0.91, "비속어 포함"),
                flagged("미친병X", 0.88, "비속어 우회 (특수문자 삽입)"),
                flagged("쓰레기같은놈들아", 0.98, "직접적 모욕 표현"),
                flagged("ㄱㅅㄲ", 0.96, "초성 비속어"),
                flagged("닥쳐이새X", 0.94, "욕설 + 특수문자 우회"),
                flagged("병신같애", 0.92, "모욕적 표현"),
                flagged("꺼져씨X", 0.93, "비속어 우회"),
                flagged("좆같네", 0.97, "직접적 성적 비속어"),
                flagged("ㅈ같은인생", 0.89, "초성+비속어 혼용"),
                flagged("개돼지들아", 0.91, "집단 모욕 표현"),
                flagged("뒤져라고", 0.95, "생명 위협 표현"),
                flagged("창X년", 0.90, "성적 모욕 우회 표현"),
                flagged("찐따냐", 0.93, "장애 비하 표현"),
                flagged("미친놈들아전부꺼져라", 0.88, "직접적 욕설"),
                flagged("씹새끼들", 0.99, "직접적 복합 욕설"),
                flagged("거지같은X", 0.87, "모욕 + 특수문자 우회"),
                flagged("ㅂㅅ집합", 0.92, "초성 비속어 집합 표현"),
                flagged("개쓰레기같은놈들아", 0.96, "동물 비유 + 모욕"),
                flagged("죽어버려", 0.94, "생명 위협"),
                flagged("재수없는놈", 0.86, "모욕적 표현"),
                flagged("X발놈아", 0.91, "특수문자 우회 욕설"),
                flagged("멍청이새X", 0.89, "복합 모욕 표현"),
                flagged("돼지같은년", 0.90, "동물 비유 + 성차별 표현"),
                flagged("ㅁㅊ새끼", 0.95, "초성 비속어"),
                flagged("인간쓰레기들아전부", 0.93, "극단적 모욕 표현"),
                flagged("꼴통들아", 0.87, "모욕적 집합 표현"),
                flagged("바보새X야", 0.88, "복합 모욕 우회 표현"));

        final List<NicknameAudit> pendingData = List.of(
                pending("열받네", 0.62, "감탄사로도 쓰이나 문맥 의존적"),
                pending("빡친호랑이", 0.71, "비속어 경계 표현"),
                pending("킹받는곰", 0.68, "신조어, 판단 불명확"),
                pending("뒤진다고", 0.75, "위협적 표현 가능성"),
                pending("X같은여우", 0.80, "특수문자 치환 의심"),
                pending("짜증유발자", 0.58, "감정 표현, 직접 욕설 아님"),
                pending("현타오는곰", 0.45, "신조어, 비속어 여부 불명확"),
                pending("빡세게살자", 0.52, "경계 표현, 문맥 의존적"),
                pending("열폭하는중", 0.67, "분노 표현, 판단 필요"),
                pending("화났어요주의하세요", 0.49, "경고성 표현, 직접 욕설 없음"),
                pending("돌아이같은", 0.73, "경계 모욕 표현"),
                pending("반쯤미친듯한기분이야", 0.70, "과장 표현, 문맥 의존"),
                pending("못살겠다야", 0.55, "극단 표현이나 일상어 가능성"),
                pending("답답한세상", 0.42, "사회 불만, 직접 비하 없음"),
                pending("갑갑한인생", 0.48, "감정 표현, 저위험"),
                pending("킹받는오리", 0.66, "신조어 혼용"),
                pending("빡세구리", 0.61, "신조어, 비속어 경계"),
                pending("뿔난코끼리", 0.44, "은유적 표현"),
                pending("화염방사기", 0.53, "공격적 이미지 연상 가능"),
                pending("독한마음곰", 0.59, "경계 표현"),
                pending("눈에띄는악당지망생", 0.64, "부정적 정체성 표현"),
                pending("악당지망생", 0.57, "부정적이나 유희적 표현 가능"),
                pending("세상원망중", 0.69, "부정적 감정, 판단 필요"),
                pending("뒤통수치기전문가야", 0.72, "부정적 의미, 직접 욕설 아님"),
                pending("나쁜짓만함", 0.76, "자기 비하인지 타인 비하인지 불명확"),
                pending("짜증덩어리", 0.63, "모욕적 표현 가능성"),
                pending("폭발직전곰", 0.50, "감정 표현, 위험도 낮음"),
                pending("욱하는성격", 0.47, "성격 표현, 직접 비하 없음"),
                pending("화병난거북이", 0.54, "과장된 감정 표현"),
                pending("비관론자야", 0.46, "부정적 관점 표현, 저위험"));

        final List<NicknameAudit> settledData = settledData();

        auditRepository.saveAll(flaggedData);
        auditRepository.saveAll(pendingData);
        auditRepository.saveAll(settledData);
        backdate(flaggedData, pendingData, settledData);
        log.info(
                "[LocalInit] 닉네임 검열 샘플 데이터 삽입 완료 - 대기 {}건, 판정 끝 {}건",
                flaggedData.size() + pendingData.size(),
                settledData.size());
    }

    /**
     * 접수 시각을 흩는다.
     *
     * <p>생성자가 {@code createdAt = Instant.now()} 를 박아서 예순 건이 전부 기동 시각에
     * 몰렸다. 검열 화면의 "접수" 열이 예순 행 모두 같은 값이라 아무 말도 하지 않았고,
     * 통합 작업함에서는 이 예순 건이 <b>최신 스무 칸을 통째로 덮어</b> 신고와 격리 메시지가
     * 한 건도 안 보였다.
     *
     * <p>도메인에 시각 주입 생성자를 열지 않는다. 그 구멍은 운영 코드에서도 쓸 수 있게 되고,
     * 그때부터 이 필드는 "기록된 시각"이 아니라 "누군가 정한 시각"이 된다.
     *
     * <p>JdbcTemplate 이 아니라 JPQL 로 쓴다. {@code Timestamp} 나 {@code LocalDateTime} 으로
     * 넘기면 드라이버의 변환 경로가 삽입 때와 달라 아홉 시간이 어긋났다. JPQL 은 Hibernate 가
     * 삽입에 쓰는 매핑을 그대로 탄다.
     */
    @SafeVarargs
    private void backdate(List<NicknameAudit>... groups) {
        final Random random = new Random(SEED);
        final Instant now = clock.instant();
        for (List<NicknameAudit> group : groups) {
            for (NicknameAudit audit : group) {
                final Instant at = now.minus(Duration.ofMinutes(random.nextInt(DAYS * 24 * 60)));
                entityManager
                        .createQuery("UPDATE NicknameAudit a SET a.createdAt = :at, a.auditedAt = :at WHERE a.id = :id")
                        .setParameter("at", at)
                        .setParameter("id", audit.getId())
                        .executeUpdate();
            }
        }
    }

    /**
     * 이미 판정이 끝난 닉네임.
     *
     * <p>대기 둘만 넣었더니 검열 화면의 판정 분포가 <b>같은 높이의 막대 두 개</b>였다.
     * 실제 서비스에서 대부분의 닉네임은 걸리지 않고 그대로 지나가고(CLEAN), 걸린 것 중
     * 일부만 사람이 막거나 풀어 준다. 그 비율이 없으면 "지금 큐에 30건"이 많은 건지
     * 적은 건지 견줄 데가 없다.
     */
    private List<NicknameAudit> settledData() {
        final List<NicknameAudit> settled = new ArrayList<>();
        final Random random = new Random(SEED);

        for (int i = 0; i < 180; i++) {
            settled.add(
                    settled("유저닉네임" + (i + 1), NicknameAuditStatus.CLEAN, 0.02 + random.nextDouble() * 0.2, "비속어 없음"));
        }
        for (int i = 0; i < 22; i++) {
            settled.add(settled(
                    "허용된닉네임" + (i + 1), NicknameAuditStatus.ALLOWED, 0.55 + random.nextDouble() * 0.25, "관리자 허용"));
        }
        for (int i = 0; i < 41; i++) {
            settled.add(settled(
                    "차단된닉네임" + (i + 1), NicknameAuditStatus.BLOCKED, 0.75 + random.nextDouble() * 0.24, "관리자 차단"));
        }
        return settled;
    }

    private NicknameAudit settled(String nickname, NicknameAuditStatus status, double confidence, String reason) {
        final NicknameAudit entity = new NicknameAudit(nickname);
        entity.complete(status, AiConfidence.of(confidence), reason);
        return entity;
    }

    private NicknameAudit flagged(String nickname, double confidence, String reason) {
        final NicknameAudit entity = new NicknameAudit(nickname);
        entity.complete(NicknameAuditStatus.FLAGGED, AiConfidence.of(confidence), reason);
        return entity;
    }

    private NicknameAudit pending(String nickname, double confidence, String reason) {
        final NicknameAudit entity = new NicknameAudit(nickname);
        entity.complete(NicknameAuditStatus.PENDING, AiConfidence.of(confidence), reason);
        return entity;
    }
}
