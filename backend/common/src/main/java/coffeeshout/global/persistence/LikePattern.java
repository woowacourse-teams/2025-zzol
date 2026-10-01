package coffeeshout.global.persistence;

/**
 * 사용자가 친 글자를 LIKE 패턴으로 바꾼다.
 *
 * <p>{@code %} 와 {@code _} 는 LIKE 에서 와일드카드다. 검색창에 친 글자를 그대로 패턴에
 * 끼우면 그 두 글자가 <b>검색어가 아니라 문법</b>으로 읽힌다. {@code %} 하나만 쳐도 전체가
 * 걸려 나오고, {@code mj_admin} 을 찾으면 {@code mjXadmin} 까지 걸린다. 관리자 이메일이나
 * 닉네임에는 밑줄이 흔해서 실제로 마주치는 자리다.
 *
 * <p>이스케이프 문자를 {@code !} 로 둔다. 기본값인 역슬래시를 쓰지 않는 이유는 MySQL 이
 * 문자열 리터럴에서도 역슬래시를 이스케이프로 보기 때문이다. {@code ESCAPE '\'} 를 SQL 로
 * 내보내면 그 자리에서 한 번 더 해석돼 방언과 설정({@code NO_BACKSLASH_ESCAPES})에 따라
 * 결과가 갈린다. {@code !} 는 어느 쪽에서도 특별한 뜻이 없다.
 *
 * <p>이스케이프 문자 자신도 이스케이프한다. 안 하면 {@code a!b} 를 찾을 때 {@code !b} 가
 * "다음 글자를 글자 그대로 봐라"로 읽혀 {@code ab} 가 걸린다.
 *
 * <p>{@code :common} 에 두는 것은 쓰는 곳이 모듈 셋에 걸쳐 있어서다(감사 로그, 유저 검색,
 * 금칙어 검색). Spring 도 JPA 도 모르는 문자열 함수라 이 모듈의 제약에 걸리지 않는다.
 */
public final class LikePattern {

    /** SQL 의 {@code ESCAPE} 절에 그대로 넘길 문자. */
    public static final char ESCAPE = '!';

    private LikePattern() {}

    /**
     * 어디에 있든 걸리는 패턴({@code %검색어%}).
     *
     * @param raw 사용자가 친 글자. 비어 있으면 {@code %%} 가 되어 전체가 걸린다. 그 판단은
     *            부르는 쪽이 한다 - 비었을 때 조건을 아예 걸지 않는 곳이 대부분이다.
     */
    public static String contains(String raw) {
        return "%" + escape(raw) + "%";
    }

    /** 와일드카드와 이스케이프 문자를 글자 그대로 바꾼다. */
    public static String escape(String raw) {
        final StringBuilder escaped = new StringBuilder(raw.length());
        for (char each : raw.toCharArray()) {
            if (each == ESCAPE || each == '%' || each == '_') {
                escaped.append(ESCAPE);
            }
            escaped.append(each);
        }
        return escaped.toString();
    }
}
