package coffeeshout.nunchi.infra;

import coffeeshout.global.redis.stream.StreamKey;
import lombok.RequiredArgsConstructor;

/**
 * 눈치게임 입력 스트림 키(ADR-0031 N1). 단일 스트림을 단일 컨슈머가 도착 순서대로 처리하며,
 * 전용 단일스레드 풀(redis.yml {@code [nunchi]} core=max=1)로 순서를 보존한다.
 */
@RequiredArgsConstructor
public enum NunchiStreamKey implements StreamKey {
    INPUT("nunchi");

    private final String redisKey;

    @Override
    public String getRedisKey() {
        return redisKey;
    }
}
