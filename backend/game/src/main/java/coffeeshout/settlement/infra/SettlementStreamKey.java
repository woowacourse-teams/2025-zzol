package coffeeshout.settlement.infra;

import coffeeshout.global.redis.stream.StreamKey;
import lombok.RequiredArgsConstructor;

/**
 * 시즌 정산 작업 큐 스트림. 브로드캐스트 스트림들과 달리 컨슈머 그룹의 단일 처리 경로로 소비한다(#1610).
 * 전달은 at-least-once이며 정산의 멱등성은 원장 유니크 키가 보장한다({@code SettlementService}).
 * redis.yml에서 listener-enabled: false로 선언되어 브로드캐스트 리스너가 생성되지 않는다.
 */
@RequiredArgsConstructor
public enum SettlementStreamKey implements StreamKey {
    RESULT("settlement:result");

    private final String redisKey;

    @Override
    public String getRedisKey() {
        return redisKey;
    }
}
