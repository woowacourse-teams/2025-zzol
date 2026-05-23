package coffeeshout.room.domain.audit;

import java.util.List;

public interface PlayerNameAuditor {

    /**
     * AI 기반 닉네임 감사를 수행합니다.
     *
     * `@param` nicknames 감사할 닉네임 목록 (null 불가, 빈 리스트 허용)
     * `@return` 입력 순서와 동일한 순서의 감사 결과 목록
     * `@throws` NicknameAuditException AI 호출 실패 시
     */
    List<PlayerNameAuditResult> audit(List<String> playerNames);
}
