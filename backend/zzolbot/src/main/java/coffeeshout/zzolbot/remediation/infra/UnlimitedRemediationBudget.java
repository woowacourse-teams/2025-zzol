package coffeeshout.zzolbot.remediation.infra;

import coffeeshout.zzolbot.remediation.application.RemediationBudget;

/**
 * Redis(StringRedisTemplate)가 없는 환경(단일 인스턴스·테스트)용 폴백. 항상 허용한다.
 * 단일 인스턴스에서는 쿨다운·화이트리스트·사람 트리거만으로도 사용량이 통제되므로 안전하다.
 */
public class UnlimitedRemediationBudget implements RemediationBudget {

    @Override
    public boolean tryAcquire() {
        return true;
    }

    @Override
    public long remaining() {
        return Long.MAX_VALUE;
    }
}
