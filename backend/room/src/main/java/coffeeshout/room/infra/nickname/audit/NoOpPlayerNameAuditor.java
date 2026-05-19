package coffeeshout.room.infra.nickname.audit;

import static java.util.Objects.requireNonNull;

import coffeeshout.room.domain.audit.AiConfidence;
import coffeeshout.room.domain.audit.PlayerNameAuditResult;
import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
import coffeeshout.room.domain.audit.PlayerNameAuditor;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("local | test")
public class NoOpPlayerNameAuditor implements PlayerNameAuditor {

    @Override
    public List<PlayerNameAuditResult> audit(List<String> playerNames) {
        log.debug("NoOpPlayerNameAuditor: Gemini 호출 생략 (local/test 프로파일), playerNames={}", playerNames);

        requireNonNull(playerNames, "playerNames은 null일 수 없습니다.");
        return playerNames.stream()
                .map(nickname -> new PlayerNameAuditResult(nickname, PlayerNameAuditStatus.CLEAN, AiConfidence.UNKNOWN, "no-op"))
                .toList();
    }
}
