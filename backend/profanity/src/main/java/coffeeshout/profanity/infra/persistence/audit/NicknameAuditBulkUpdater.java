package coffeeshout.profanity.infra.persistence.audit;

import coffeeshout.profanity.domain.audit.NicknameAudit;
import java.util.List;

/**
 * {@code NicknameAuditJpaRepository}가 함께 extend하는 Spring Data 커스텀 프래그먼트.
 * 구현체는 {@code NicknameAuditBulkUpdaterImpl}(이름 규약)이 맡는다.
 */
public interface NicknameAuditBulkUpdater {

    void bulkUpdateAuditResults(List<NicknameAudit> entities);
}
