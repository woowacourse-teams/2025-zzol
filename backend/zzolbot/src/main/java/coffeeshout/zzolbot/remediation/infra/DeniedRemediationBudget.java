package coffeeshout.zzolbot.remediation.infra;

import coffeeshout.zzolbot.remediation.application.RemediationBudget;

/**
 * Redis(StringRedisTemplate)가 없을 때의 fail-closed 폴백. 항상 거부한다.
 *
 * <p>자동 수정은 실코드를 바꾸는 PR을 만들므로 일일 디스패치 캡이 안전장치다. Redis 배선 누락으로 캡이
 * 사라지면(무제한) 위험하므로, 캡을 강제할 수 없는 상황에서는 차라리 디스패치를 막는다. 운영은 Redis가
 * 항상 있으니 이 폴백은 오설정(enabled=true인데 Redis 부재) 시에만 닿고, 그때 안전하게 잠근다.
 */
public class DeniedRemediationBudget implements RemediationBudget {

    @Override
    public boolean tryAcquire() {
        return false;
    }

    @Override
    public long remaining() {
        return 0;
    }
}
