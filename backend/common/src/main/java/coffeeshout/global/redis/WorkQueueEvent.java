package coffeeshout.global.redis;

/**
 * 컨슈머 그룹(작업 큐)으로 소비되는 이벤트 마커(#1610). 브로드캐스트 파이프라인을 타지 않으므로
 * "모든 이벤트는 {@code Consumer<T>} 빈을 가진다"는 기동 검증(ADR-0025)에서 제외된다.
 * 소비 책임은 해당 스트림의 컨슈머 그룹 소유 컴포넌트가 진다.
 */
public interface WorkQueueEvent extends BaseEvent {
}
