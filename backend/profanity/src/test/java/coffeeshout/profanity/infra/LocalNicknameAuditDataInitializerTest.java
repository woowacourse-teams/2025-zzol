package coffeeshout.profanity.infra;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.fixture.NicknameAuditPropertiesFixture;
import coffeeshout.support.ServiceTest;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 측정용 미검열 닉네임 적재. 10만 건을 넣고 회차를 돌려 파이프라인 처리량을 재는 것이 목적이라
 * 건수가 프로퍼티대로 들어가고 다시 띄워도 안 늘어나야 한다.
 */
class LocalNicknameAuditDataInitializerTest extends ServiceTest {

    /** 청크 경계(1000)를 두 번 넘고 마지막 청크가 덜 차는 값. 나머지를 빠뜨리면 여기서 걸린다. */
    private static final int 적재_건수 = 2_500;

    @Autowired
    private NicknameAuditRepository auditRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private LocalNicknameAuditDataInitializer 초기화기(int seedCount) {
        return new LocalNicknameAuditDataInitializer(
                auditRepository,
                NicknameAuditPropertiesFixture.적재(seedCount),
                jdbcTemplate,
                Clock.systemUTC(),
                entityManager,
                transactionTemplate);
    }

    private long 미검열_건수() {
        return auditRepository.countByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED);
    }

    @Nested
    class seed_count_적재 {

        @Test
        void 설정한_건수만큼_미검열_닉네임이_들어간다() {
            초기화기(적재_건수).run(null);

            assertThat(미검열_건수()).isEqualTo(적재_건수);
        }

        @Test
        void 두_번_돌려도_건수가_늘지_않는다() {
            초기화기(적재_건수).run(null);
            초기화기(적재_건수).run(null);

            assertThat(미검열_건수()).as("앱을 다시 띄울 때마다 적체가 불어나면 회차 간 측정값을 비교할 수 없다.").isEqualTo(적재_건수);
        }

        @Test
        void 회차가_끝나_승격된_뒤에도_같은_이름을_다시_넣지_않는다() {
            초기화기(적재_건수).run(null);
            jdbcTemplate.update(
                    "UPDATE player_name_audit SET status = 'CLEAN', audited_at = NOW() WHERE player_name LIKE 'zz%'");

            초기화기(적재_건수).run(null);

            assertThat(미검열_건수())
                    .as("UNAUDITED 건수로만 막으면 회차를 한 번 끝낸 뒤 같은 이름이 다시 들어간다."
                            + " 그 행들은 다음 회차에서 판정 대신 중복 재등록으로 지워져 측정이 삭제 경로를 잰다.")
                    .isZero();
        }

        @Test
        void 기본값_0이면_아무것도_넣지_않는다() {
            초기화기(0).run(null);

            assertThat(미검열_건수()).isZero();
        }
    }

    /**
     * 실제 기동에는 주변 트랜잭션이 없다. 베이스의 테스트 트랜잭션 안에서만 돌리면 접수 시각을 흩는 벌크
     * UPDATE가 그 트랜잭션에 얹혀 통과하고, 로컬 기동은 TransactionRequiredException으로 죽는다(#1859).
     * 그래서 이 묶음만 테스트 트랜잭션을 끄고, 커밋된 행은 직접 지운다. 앞선 테스트가 남긴 대기 행이
     * 있으면 샘플 적재가 통째로 건너뛰어지므로 시작 전에도 비운다.
     */
    @Nested
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    class 트랜잭션_없는_기동 {

        @BeforeEach
        void 남은_행을_지운다() {
            cleanDatabase();
        }

        @AfterEach
        void 커밋된_행을_지운다() {
            cleanDatabase();
        }

        @Test
        void 샘플_데이터를_넣고_접수_시각을_흩는다() {
            // 스프링이 기동 때 부르는 호출 그대로다. null 자리는 기동 인자인데 시더가 읽지 않는다.
            // 시더가 스스로 트랜잭션을 열지 않으면 이 줄에서 TransactionRequiredException이 올라와 실패한다.
            초기화기(0).run(null);

            // 시더가 쓴 것과 같은 JPQL 매핑으로 결과만 읽는다. JDBC로 읽으면 시간대 변환이 달라진다.
            final Instant 가장_이른_접수 = entityManager
                    .createQuery("SELECT MIN(a.createdAt) FROM NicknameAudit a", Instant.class)
                    .getSingleResult();
            assertThat(가장_이른_접수)
                    .as("접수 시각이 전부 기동 시각에 몰리면 검열 화면의 접수 열이 아무 말도 하지 않는다.")
                    .isBefore(Instant.now().minus(Duration.ofDays(1)));
        }
    }
}
