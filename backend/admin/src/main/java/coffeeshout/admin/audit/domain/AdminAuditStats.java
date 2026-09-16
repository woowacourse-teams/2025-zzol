package coffeeshout.admin.audit.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * 조치 이력 화면 상단의 그래프 재료.
 *
 * <p>표는 "이 조치가 무엇이었나"에 답한다. 감사 로그를 여는 이유는 대개 그 하나를 찾으려는
 * 것이지만, 찾기 전에 알아야 하는 것이 둘 있다. <b>실패가 늘고 있는가</b>와 <b>어떤
 * 조치가 실제로 쓰이는가</b>다. 실패는 되돌려야 할 일이 있다는 뜻이고, 쓰이지 않는 조치는
 * 화면에서 뺄 후보다.
 *
 * @param total     기간 안의 조치
 * @param failed    그중 실패한 조치
 * @param actions   조치 종류별 수. 많은 순
 * @param actors    담당자별 수. 많은 순. 다섯을 넘으면 나머지는 하나로 묶는다
 * @param daily     일자별 성공과 실패. 조치가 없는 날도 0으로 채운다
 */
public record AdminAuditStats(
        long total, long failed, List<ActionCount> actions, List<ActorCount> actors, List<DailyCount> daily) {

    public record ActionCount(String action, long count) {}

    public record ActorCount(String actorEmail, long count) {}

    public record DailyCount(LocalDate date, long success, long failure) {}
}
