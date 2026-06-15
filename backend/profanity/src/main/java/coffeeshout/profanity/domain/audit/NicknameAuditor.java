package coffeeshout.profanity.domain.audit;

import java.util.List;

public interface NicknameAuditor {
    List<NicknameAuditResult> audit(List<String> nicknames);
}
