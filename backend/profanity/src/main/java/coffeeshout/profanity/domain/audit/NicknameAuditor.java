package coffeeshout.profanity.domain.audit;

import java.util.List;

public interface NicknameAuditor {

    /**
     * 닉네임 목록을 판정한다.
     *
     * <p>결과는 요청한 닉네임의 일부일 수 있다. 판정을 붙이지 못한 닉네임은 결과에서 빠지고, 호출자가 시도 횟수를
     * 올려 다시 판정받게 한다.
     */
    List<NicknameAuditResult> audit(List<String> nicknames);
}
