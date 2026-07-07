package coffeeshout.profanity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.infra.persistence.audit.NicknameAuditJpaRepository;
import coffeeshout.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 스케줄러가 잔존 UNAUDITED 행을 검열할 때 유니크 제약({@code uq_player_name_audit_name_status})에
 * 충돌해 배치 전체가 롤백되며 큐가 영구히 적체되던 장애의 회귀 테스트.
 *
 * <p>비트랜잭션 베이스({@link IntegrationTestSupport})를 상속해 실제 commit 시점에 제약이 검증되도록 한다.
 * @Transactional 롤백 베이스에서는 flush가 지연돼 제약 위반이 재현되지 않는다.
 */
class ProfanityAuditServiceDuplicateKeyIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProfanityAuditService service;

    @Autowired
    private NicknameAuditJpaRepository auditRepository;

    @Test
    void 이미_CLEAN인_닉네임의_잔존_UNAUDITED를_검열해도_유니크_충돌_없이_큐가_비워진다() {
        // #1467 fix 이전에 생성된 잔존 데이터 재현: 같은 닉네임이 (CLEAN)과 (UNAUDITED)로 공존한다.
        // uq_player_name_audit_name_status는 (player_name, status)라 status가 달라 둘 다 insert된다.
        final NicknameAudit alreadyClean = new NicknameAudit("host");
        alreadyClean.complete(NicknameAuditStatus.CLEAN, AiConfidence.UNKNOWN, "기존 검열 완료");
        auditRepository.save(alreadyClean);
        auditRepository.save(new NicknameAudit("host")); // 잔존 UNAUDITED 중복

        // NoOpNicknameAuditor(test 프로파일)가 CLEAN을 반환 → UNAUDITED 'host'를 CLEAN으로 승격 시도.
        // 기존 (host, CLEAN)과 충돌하므로, 승격 대신 중복 재등록으로 인지해 제거하고 예외 없이 큐를 비워야 한다.
        assertThatCode(service::auditPending).doesNotThrowAnyException();

        assertThat(auditRepository.countByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED)).isZero();
        assertThat(auditRepository.findNicknamesByStatus(NicknameAuditStatus.CLEAN)).containsOnly("host");
    }
}
