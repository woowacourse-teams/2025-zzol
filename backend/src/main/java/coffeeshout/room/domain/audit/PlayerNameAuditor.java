package coffeeshout.room.domain.audit;

import java.util.List;

public interface PlayerNameAuditor {

    List<PlayerNameAuditResult> audit(List<String> playerNames);
}
