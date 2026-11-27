package coffeeshout.profanity.fixture;

import coffeeshout.profanity.domain.audit.NicknameAudit;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * id가 있는 UNAUDITED 행을 만든다. {@code new NicknameAudit(...)}는 id가 null이라 id로 행을 가르는 로직을 검증할 수 없다.
 */
public final class NicknameAuditFixture {

    private NicknameAuditFixture() {}

    public static NicknameAudit 미검열(long id, String nickname) {
        final NicknameAudit audit = new NicknameAudit(nickname);
        ReflectionTestUtils.setField(audit, "id", id);
        return audit;
    }
}
