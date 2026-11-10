package coffeeshout.profanity.application;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.infra.persistence.audit.NicknameAuditJpaRepository;
import coffeeshout.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 랭킹에 노출된 미등록 닉네임이 검열 큐에 들어가는지 실제 트랜잭션 경계에서 검증한다 (#1906).
 *
 * <p>랭킹 조회는 읽기 전용 트랜잭션이라 그 안에서 INSERT하면 MySQL이 거부한다. 등록은 조회 트랜잭션이 끝난 뒤
 * 별도 트랜잭션에서 일어나야 하고, {@code @Transactional} 롤백 베이스에서는 그 경계를 볼 수 없다.
 */
class NicknameExposureRegistrationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private NicknameExposureService exposureService;

    @Autowired
    private NicknameAuditJpaRepository auditRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void 읽기_전용_조회가_끝나면_행이_없던_닉네임을_UNAUDITED로_등록한다() {
        final TransactionTemplate readOnly = new TransactionTemplate(transactionManager);
        readOnly.setReadOnly(true);

        readOnly.executeWithoutResult(status -> {
            exposureService.findRevealable(List.of("랭킹새닉"));

            // 조회 트랜잭션 안에서는 아직 등록되지 않는다. 등록이 이 트랜잭션에 참여했다면 읽기 전용 위반으로 깨진다.
            assertThat(auditRepository.existsByNickname("랭킹새닉")).isFalse();
        });

        assertThat(auditRepository.findNicknamesByStatus(NicknameAuditStatus.UNAUDITED))
                .containsOnly("랭킹새닉");
    }
}
