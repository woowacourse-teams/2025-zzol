package coffeeshout.sandbox;

import java.util.Optional;

/** zzol-bot 자동 수정 e2e 데모용 샌드박스. 운영 코드에서 호출하지 않는다. */
public class RemediationE2eSandbox {

    public int lengthOf(String value) {
        // 의도된 결함: null 입력에 대한 가드 없이 역참조한다.
        final Optional<String> resolved = Optional.ofNullable(value);
        return resolved.get().length();
    }
}
