package coffeeshout.fixture;

import coffeeshout.global.lock.RedisLock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @RedisLock 애스펙트 검증용 락 대상 작업.
 * <p>
 * 실행 횟수를 세고, holdNextExecution()이 호출된 상태면 메서드 안에서(락을 보유한 채로) 대기해
 * 테스트가 동시성 시나리오의 타이밍을 결정론적으로 제어할 수 있게 한다.
 */
public class RedisLockedTaskFake {

    public static final String LOCK_PREFIX = "test:lock:";
    public static final String DONE_PREFIX = "test:done:";

    private final AtomicInteger executionCount = new AtomicInteger();
    private volatile CountDownLatch enteredLatch = new CountDownLatch(1);
    private volatile CountDownLatch holdLatch;

    @RedisLock(key = "#eventId", lockPrefix = LOCK_PREFIX, donePrefix = DONE_PREFIX)
    public String execute(String eventId) {
        executionCount.incrementAndGet();
        awaitHoldIfSet();
        return eventId;
    }

    @RedisLock(key = "#eventId", lockPrefix = LOCK_PREFIX, donePrefix = DONE_PREFIX, waitTime = 3000)
    public String executeWithLockWait(String eventId) {
        executionCount.incrementAndGet();
        awaitHoldIfSet();
        return eventId;
    }

    @RedisLock(key = "#eventId", lockPrefix = LOCK_PREFIX, donePrefix = DONE_PREFIX)
    public String executeFailing(String eventId) {
        executionCount.incrementAndGet();
        throw new IllegalStateException("의도된 실패");
    }

    public void reset() {
        executionCount.set(0);
        enteredLatch = new CountDownLatch(1);
        holdLatch = null;
    }

    /**
     * 다음 실행을 메서드 내부(락 보유 상태)에서 releaseHeldExecution() 호출까지 붙잡아 둔다.
     */
    public void holdNextExecution() {
        holdLatch = new CountDownLatch(1);
    }

    public void releaseHeldExecution() {
        final CountDownLatch latch = holdLatch;
        if (latch != null) {
            latch.countDown();
        }
    }

    /**
     * 어떤 스레드가 메서드 본문에 진입할 때까지 대기한다 (락 보유 확정 시점).
     */
    public boolean awaitEntered(long timeout, TimeUnit unit) throws InterruptedException {
        return enteredLatch.await(timeout, unit);
    }

    public int executionCount() {
        return executionCount.get();
    }

    private void awaitHoldIfSet() {
        enteredLatch.countDown();
        final CountDownLatch latch = holdLatch;
        if (latch == null) {
            return;
        }
        try {
            // leaseTime(기본 5초) 만료 전에 반드시 풀리도록 상한을 둔다
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
