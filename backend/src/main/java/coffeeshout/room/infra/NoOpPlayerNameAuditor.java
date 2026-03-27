package coffeeshout.room.infra;

import coffeeshout.room.domain.audit.PlayerNameAuditResult;
import coffeeshout.room.domain.audit.PlayerNameAuditor;
import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("local | test")
public class NoOpPlayerNameAuditor implements PlayerNameAuditor {

    @Override
    public List<PlayerNameAuditResult> audit(List<String> nicknames) {
        log.debug("NoOpNicknameAuditor: Gemini 호출 생략 (local/test 프로파일), nicknames={}", nicknames);
        return nicknames.stream()
                .map(nickname -> new PlayerNameAuditResult(nickname, PlayerNameAuditStatus.CLEAN, 0.0, "no-op"))
                .toList();
    }
}
