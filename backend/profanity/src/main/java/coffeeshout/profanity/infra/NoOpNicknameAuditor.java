package coffeeshout.profanity.infra;

import static java.util.Objects.requireNonNull;

import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameAuditResult;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.domain.audit.NicknameAuditor;
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

        requireNonNull(nicknames, "nicknames은 null일 수 없습니다.");
        return nicknames.stream()
                .map(nickname -> new NicknameAuditResult(nickname, NicknameAuditStatus.CLEAN, AiConfidence.UNKNOWN, "no-op"))
                .toList();
    }
}
