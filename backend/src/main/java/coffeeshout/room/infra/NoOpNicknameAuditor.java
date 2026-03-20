package coffeeshout.room.infra;

import coffeeshout.room.domain.audit.NicknameAuditResult;
import coffeeshout.room.domain.audit.NicknameAuditor;
import coffeeshout.room.domain.audit.NicknameAuditStatus;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("local | test")
public class NoOpNicknameAuditor implements NicknameAuditor {

    @Override
    public List<NicknameAuditResult> audit(List<String> nicknames) {
        log.debug("NoOpNicknameAuditor: Gemini 호출 생략 (local/test 프로파일), nicknames={}", nicknames);
        return nicknames.stream()
                .map(nickname -> new NicknameAuditResult(nickname, NicknameAuditStatus.CLEAN, 0.0, "no-op"))
                .toList();
    }
}
