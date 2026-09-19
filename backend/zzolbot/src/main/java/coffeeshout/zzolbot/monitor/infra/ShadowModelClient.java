package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import java.util.List;

/**
 * 섀도우 비교에 쓰는 자체 호스팅 모델. <b>원 응답을 그대로</b> 돌려준다.
 *
 * <p>{@code AnomalyAnalyzer}가 아니라 별도 포트인 이유가 둘이다. 하나는 이 경로의 결과가 운영
 * 판정에 쓰이지 않아 분석기로 주입될 일이 없다는 것이고, 다른 하나는 기록이 <b>접지 전</b> 주장과
 * 인용 원문을 봐야 해서 파싱된 분석만으로는 부족하다는 것이다.
 */
public interface ShadowModelClient {

    String generate(FiringAlert alert, List<String> logSamples, String logEnvironment);
}
