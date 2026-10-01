package coffeeshout.admin.overview.domain;

import java.time.LocalDate;

/**
 * 하루치 흐름 한 점.
 *
 * <p>"오늘 방이 0개인데 이게 정상인가"에 답하려면 어제와 지난주가 있어야 한다.
 * 오늘 숫자만 보여주는 대시보드는 매번 사람이 기억에 의존하게 만든다.
 *
 * @param completed 완주한 방(DONE). created 와 함께 보면 그날의 완주율이 나온다.
 */
public record DailyTrendPoint(LocalDate date, long created, long completed, long players) {}
