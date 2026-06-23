package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.monitor.application.LlmCallBudget;

/**
 * Redis(StringRedisTemplate)가 없는 환경(단일 인스턴스·테스트)용 폴백. 항상 허용한다.
 * 단일 인스턴스에서는 cooldown·임계값 게이팅만으로도 사용량이 통제되므로 안전하다.
 */
public class UnlimitedLlmCallBudget implements LlmCallBudget {

    @Override
    public boolean tryAcquire() {
        return true;
    }

    @Override
    public long remaining() {
        return Long.MAX_VALUE;
    }
}
