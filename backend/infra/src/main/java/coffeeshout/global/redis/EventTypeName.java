package coffeeshout.global.redis;

/**
 * 이벤트의 관측성용 타입 식별자를 파생하는 인프라 헬퍼. 트레이스 span 이름·로그에 쓴다.
 *
 * <p>{@code getClass().getSimpleName()}을 직접 쓰면 중첩 record가 {@code Created}처럼 외부 맥락을
 * 잃어 패밀리 간 충돌·식별 곤란이 생긴다. 중첩 이벤트는 외부 클래스로 한정해 모호성을 없앤다
 * (예: {@code RoomLifecycleEvent.Created}), 평탄 이벤트는 단순 클래스명을 그대로 쓴다.
 *
 * <p>타입명 파생은 직렬화·관측성에서 쓰이는 인프라 관심사이므로 도메인 계약({@code BaseEvent})에
 * 두지 않고 이 헬퍼로 일원화한다(어노테이션 불필요).
 */
public final class EventTypeName {

    private EventTypeName() {
    }

    public static String of(BaseEvent event) {
        return of(event.getClass());
    }

    public static String of(Class<?> eventType) {
        final Class<?> enclosing = eventType.getEnclosingClass();
        return enclosing == null
                ? eventType.getSimpleName()
                : enclosing.getSimpleName() + "." + eventType.getSimpleName();
    }
}
