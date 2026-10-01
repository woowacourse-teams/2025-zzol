package coffeeshout.profanity.infra;

import coffeeshout.profanity.application.port.NicknameFeedbackRepository;
import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameFeedback;
import coffeeshout.profanity.domain.audit.NicknameFeedback.OperatorDecision;
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

/**
 * 로컬 전용 닉네임 검열 피드백 시드.
 *
 * <p>백오피스의 "검열 판정 정확도"는 {@code player_name_feedback} 의 네 조합을 센다.
 * AI가 걸렀는데 운영자가 허용하면 오탐, AI가 안 걸렀는데 운영자가 차단하면 미탐이다.
 * 이 표를 안 채우면 로컬에서 그 카드가 늘 0 이라 <b>화면에서 판단할 수 있는 것이 없다.</b>
 * 숫자가 몇 자리로 찍히는지, 번복률이 한 자리인지 두 자리인지, 네 칸이 나란히 섰을 때
 * 시선 높이가 맞는지는 값이 있어야만 보인다.
 *
 * <p>분포를 일부러 기울였다. 고르게 뽑으면 오탐과 미탐이 비슷하게 나오는데, 실제로는
 * 신뢰도 0.85 위에서 자동 차단하므로 <b>오탐이 미탐보다 많다.</b> 그 모양이 나와야 카드가
 * "어느 쪽으로 틀리는가"를 말한다. 양쪽이 같으면 카드가 아무 말도 하지 않는다.
 *
 * <p>닉네임 시더({@code LocalNicknameAuditDataInitializer})와 나누어 둔다. 대기 목록과
 * 판정 이력은 다른 표이고 각자 멱등성 기준이 달라서, 한 러너에 묶으면 한쪽만 채워진 채로
 * 다른 쪽이 건너뛰어진다.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalNicknameFeedbackDataInitializer implements ApplicationRunner {

    /** 30일치를 만든다. 백오피스 품질 지표의 기본 기간이 30일이다. */
    private static final int DAYS = 30;

    /** 고정 씨앗. 기동할 때마다 같은 데이터가 나와야 화면 변경을 비교할 수 있다. */
    private static final long SEED = 20_260_912L;

    private static final int AGREED_BLOCKED_COUNT = 72;
    private static final int AGREED_ALLOWED_COUNT = 45;

    /**
     * AI가 걸렀고 운영자도 차단했다. 판정 일치.
     *
     * <p>{@code LocalNicknameAuditDataInitializer} 의 FLAGGED 목록과 겹치게 두었다. 대기 목록에서
     * 처리한 것이 판정 이력으로 넘어오는 흐름이라, 두 표의 닉네임이 아예 다르면 화면을 오갈 때
     * 서로 다른 서비스처럼 보인다.
     */
    private static final List<String> AGREED_BLOCKED = List.of(
            "씨b알", "ㅅㅂ놈아", "개새끼야", "존나빠름", "미친병X", "쓰레기같은놈", "ㄱㅅㄲ", "닥쳐이새X", "병신같애", "꺼져씨X", "좆같네", "ㅈ같은인생", "개돼지들아",
            "뒤져라고", "창X년", "찐따냐", "씹새끼들", "ㅂㅅ집합");

    /**
     * AI가 안 걸렀고 운영자도 허용했다. 판정 일치.
     *
     * <p>길이를 일부러 흩었다. 두 글자부터 열 글자까지 섞여 있어야 이 값이 표에 실렸을 때
     * 열 너비를 어디에 맞출지 정할 수 있다. 전부 두 글자면 어떤 폭을 줘도 맞아 보인다.
     */
    private static final List<String> AGREED_ALLOWED = List.of(
            "민준",
            "서연이",
            "도윤도윤",
            "커피한잔더",
            "오늘도맑음이야",
            "지호",
            "수아",
            "달려라코끼리야",
            "예준",
            "채원채원채원",
            "시우",
            "눈치백단플레이어",
            "건우",
            "유나",
            "주말의방장님");

    /**
     * AI가 걸렀는데 운영자가 허용했다. <b>오탐.</b>
     *
     * <p>멀쩡한 말인데 비속어 조각을 품은 것들로 골랐다. "개", "존", "빡", "죽" 같은 음절이
     * 그대로 들어 있어서 필터가 걸고 사람이 푸는, 실제로 가장 자주 일어나는 종류다.
     * 신뢰도도 임계값 바로 위(0.85~0.92)에 몰아 두었다. 오탐은 확신이 약한 자리에서 난다.
     */
    private static final List<Sample> FALSE_POSITIVE = List.of(
            new Sample("개발자입니다", 0.89, "'개' 음절 오검출. 직업 표기"),
            new Sample("존맛탱구리", 0.87, "'존' 음절 오검출. 음식 신조어"),
            new Sample("빡공하는중", 0.86, "'빡' 음절 오검출. 학습 표현"),
            new Sample("죽순이좋아", 0.91, "'죽' 음절 오검출. 식재료"),
            new Sample("개나리꽃밭", 0.88, "'개' 음절 오검출. 식물명"),
            new Sample("미친듯이달려", 0.90, "강조 부사. 대상 없는 자기 표현"),
            new Sample("존버중입니다", 0.85, "투자 신조어. 비하 대상 없음"),
            new Sample("빡세게가보자", 0.86, "강도 표현. 공격성 없음"),
            new Sample("개똥벌레야", 0.92, "'개' 음절 오검출. 곤충명"),
            new Sample("죽방렴장인", 0.88, "'죽' 음절 오검출. 전통 어업"),
            new Sample("개운한아침", 0.87, "'개' 음절 오검출. 상태 표현"),
            new Sample("존경합니다", 0.90, "'존' 음절 오검출. 존대 표현"),
            new Sample("미친실력자", 0.89, "감탄 표현. 칭찬 맥락"),
            new Sample("빡빡이아저씨", 0.85, "외형 표현이나 자기 지칭"),
            new Sample("개편한하루", 0.86, "'개' 음절 오검출. 강조 접두"));

    /**
     * AI가 안 걸렀는데 운영자가 차단했다. <b>미탐.</b>
     *
     * <p>숫자와 글자를 바꿔치기한 우회들이다. 임계값 아래로 빠져나가는 종류라 신뢰도가 낮다.
     * 미탐을 오탐보다 적게 둔 것은 임계값을 낮게 잡은 시스템의 모양을 따른 것이다.
     */
    private static final List<Sample> FALSE_NEGATIVE = List.of(
            new Sample("ㅅ1발놈", 0.41, "숫자 치환 우회. 자동 판정 통과"),
            new Sample("씨8럴", 0.38, "숫자 치환 우회"),
            new Sample("ㅂ1ㅅ아", 0.44, "초성에 숫자 삽입"),
            new Sample("개ㅅㄲ야", 0.52, "초성 혼용 우회"),
            new Sample("죽0라진짜", 0.35, "숫자 치환 + 위협 표현"),
            new Sample("ㅗㅗ가세요", 0.29, "기호 모욕. 사전 미등재"),
            new Sample("뒤질래진짜", 0.48, "위협 표현. 임계값 미달"),
            new Sample("호구새1끼", 0.46, "숫자 삽입 우회"));

    /** 일치 판정에는 사유를 길게 적지 않는다. 실제로도 운영자는 뒤집을 때만 이유를 쓴다. */
    private static final List<String> AGREED_BLOCKED_REASONS =
            List.of("AI 판정 유지", "직접적 욕설 확인", "우회 표현 확인", "재검토 후 차단 유지");

    private static final List<String> AGREED_ALLOWED_REASONS = List.of("정상 닉네임", "AI 미검출 확인", "문제 없음");

    private final NicknameFeedbackRepository feedbackRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (feedbackRepository.count() > 0) {
            log.info("[LocalInit] 닉네임 판정 이력이 이미 존재합니다. 초기 데이터 삽입을 건너뜁니다.");
            return;
        }

        final Random random = new Random(SEED);
        final List<Sample> samples = buildSamples(random);
        final Instant now = clock.instant();

        for (Sample sample : samples) {
            final NicknameFeedback saved = feedbackRepository.save(sample.toEntity());
            backdate(saved, now, random);
        }

        log.info(
                "[LocalInit] 닉네임 판정 이력 삽입 완료 — 전체 {}건 (오탐 {}건, 미탐 {}건)",
                samples.size(),
                FALSE_POSITIVE.size(),
                FALSE_NEGATIVE.size());
    }

    private List<Sample> buildSamples(Random random) {
        final List<Sample> samples = new ArrayList<>();

        for (int i = 0; i < AGREED_BLOCKED_COUNT; i++) {
            samples.add(new Sample(
                    AGREED_BLOCKED.get(i % AGREED_BLOCKED.size()),
                    true,
                    0.86 + random.nextDouble() * 0.13,
                    OperatorDecision.BLOCKED,
                    pick(AGREED_BLOCKED_REASONS, random)));
        }
        for (int i = 0; i < AGREED_ALLOWED_COUNT; i++) {
            samples.add(new Sample(
                    AGREED_ALLOWED.get(i % AGREED_ALLOWED.size()),
                    false,
                    random.nextDouble() * 0.35,
                    OperatorDecision.ALLOWED,
                    pick(AGREED_ALLOWED_REASONS, random)));
        }
        samples.addAll(FALSE_POSITIVE.stream()
                .map(sample -> sample.withDecision(true, OperatorDecision.ALLOWED))
                .toList());
        samples.addAll(FALSE_NEGATIVE.stream()
                .map(sample -> sample.withDecision(false, OperatorDecision.BLOCKED))
                .toList());

        return samples;
    }

    /**
     * 생성자가 {@code createdAt = Instant.now()} 를 박으므로 저장한 뒤에 시각을 되돌린다.
     *
     * <p>시더를 위해 도메인에 시각 주입 생성자를 열지 않는다. 그 구멍은 운영 코드에서도
     * 쓸 수 있게 되고, 그때부터 이 필드는 "기록된 시각"이 아니라 "누군가 정한 시각"이 된다.
     *
     * <p>전부 기동 시각에 몰려 있으면 7일 구간과 30일 구간이 같은 숫자를 낸다. 기간 탭이
     * 동작하는지를 화면에서 확인할 수 없게 된다.
     *
     * <p>JdbcTemplate 이 아니라 JPQL 로 쓴다. {@code Timestamp} 나 {@code LocalDateTime} 으로
     * 넘기면 드라이버의 변환 경로가 삽입 때와 달라 같은 컬럼인데 아홉 시간이 어긋났고,
     * 화면에 <b>미래 시각</b>이 찍혔다. JPQL 은 Hibernate 가 삽입에 쓰는 매핑을 그대로 타므로
     * 시간대를 손으로 계산할 일이 없다.
     */
    private void backdate(NicknameFeedback feedback, Instant now, Random random) {
        final Instant judgedAt = now.minus(Duration.ofMinutes(random.nextInt(DAYS * 24 * 60)));
        entityManager
                .createQuery("UPDATE NicknameFeedback f SET f.createdAt = :at WHERE f.id = :id")
                .setParameter("at", judgedAt)
                .setParameter("id", feedback.getId())
                .executeUpdate();
    }

    private static String pick(List<String> candidates, Random random) {
        return candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * 표에 넣을 한 줄. {@code aiFlagged} 와 결정은 목록이 정해 주므로 뒤에서 채운다.
     */
    private record Sample(
            String nickname, boolean aiFlagged, double confidence, OperatorDecision decision, String reason) {

        private Sample(String nickname, double confidence, String reason) {
            this(nickname, false, confidence, OperatorDecision.ALLOWED, reason);
        }

        private Sample withDecision(boolean aiFlagged, OperatorDecision decision) {
            return new Sample(nickname, aiFlagged, confidence, decision, reason);
        }

        private NicknameFeedback toEntity() {
            return new NicknameFeedback(nickname, aiFlagged, AiConfidence.of(confidence), decision, reason);
        }
    }
}
