package coffeeshout.global.redis;

public interface EventHandler <T> {

    void handle(T event);

    Class<T> eventType();
}
