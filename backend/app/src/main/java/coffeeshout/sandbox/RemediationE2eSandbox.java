package coffeeshout.sandbox;

import java.util.Optional;

/** zzol-bot 자동 수정 e2e 데모용 샌드박스. 운영 코드에서 호출하지 않는다. */
public class RemediationE2eSandbox {

    public int lengthOf(String value) {
        // 의도된 결함: null 입력에 대한 가드 없이 역참조한다.
        // 수정: Optional.map과 orElse를 사용하여 NullPointerException을 방지하고, null 입력 시 0을 반환한다.
        return Optional.ofNullable(value)
                       .map(String::length)
                       .orElse(0);
    }
}