package coffeeshout.profanity.infra;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.fixture.NicknameAuditPropertiesFixture;
import coffeeshout.support.ServiceTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

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

    private LocalNicknameAuditDataInitializer 초기화기(int seedCount) {
        return new LocalNicknameAuditDataInitializer(
                auditRepository, NicknameAuditPropertiesFixture.적재(seedCount), jdbcTemplate);
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
                    "UPDATE player_name_audit SET status = 'CLEAN', audited_at = NOW() WHERE player_name LIKE '측정%'");

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
}
