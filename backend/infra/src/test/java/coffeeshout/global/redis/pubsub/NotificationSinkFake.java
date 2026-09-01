package coffeeshout.global.redis.pubsub;

import coffeeshout.global.notify.NotificationSink;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 전달된 알림을 기록하는 fake sink. 수신이 비동기라 {@link #awaitDelivery(long)}로 기다린다
 * (sleep 없이 도착 시점에 깨어난다).
 * <p>
 * 전달 시점의 traceId를 함께 기록한다 — 구독자가 봉투의 traceparent로 consumer 스코프를 열었는지는
 * sink가 실행되는 스레드의 활성 스팬으로만 확인할 수 있다.
 */
public class NotificationSinkFake implements NotificationSink {

    private final List<Delivered> deliveries = new CopyOnWriteArrayList<>();
    private final Supplier<String> currentTraceIdSupplier;
    private volatile CountDownLatch latch = new CountDownLatch(1);

    public NotificationSinkFake() {
        this(() -> null);
    }

    public NotificationSinkFake(Supplier<String> currentTraceIdSupplier) {
        this.currentTraceIdSupplier = currentTraceIdSupplier;
    }

    @Override
    public void deliver(String destination, String payloadJson) {
        deliveries.add(new Delivered(destination, payloadJson, currentTraceIdSupplier.get()));
        latch.countDown();
    }

    /**
     * 다음 전달을 기다릴 준비를 한다. 발행 <b>전에</b> 호출한다.
     */
    public void expectDeliveries(int count) {
        latch = new CountDownLatch(count);
    }

    public boolean awaitDelivery(long timeoutSeconds) throws InterruptedException {
        return latch.await(timeoutSeconds, TimeUnit.SECONDS);
    }

    public List<Delivered> deliveries() {
        return List.copyOf(deliveries);
    }

    public void clear() {
        deliveries.clear();
    }

    public record Delivered(String destination, String payloadJson, String traceId) {}
}
