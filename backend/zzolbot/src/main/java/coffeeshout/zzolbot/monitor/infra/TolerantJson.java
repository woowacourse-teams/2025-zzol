package coffeeshout.zzolbot.monitor.infra;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * 모델이 얽힌 JSON을 읽는 전용 파서. 문자열 안의 raw 제어문자를 거부하지 않는다.
 *
 * <p><b>왜 프레임워크 컨버터를 안 쓰나.</b> 두 가지 이유가 있고 둘 다 실제로 겪었다.
 *
 * <ul>
 *   <li>Spring Boot 4의 HTTP 메시지 컨버터는 Jackson 3({@code tools.jackson}) 기반이라
 *       이 모듈이 쓰는 Jackson 2의 추상 타입을 만들지 못한다(#1813)</li>
 *   <li>모델은 로그 줄을 그대로 옮겨 적는다. 그 줄의 제어문자를 이스케이프하리라는 보장이 없고,
 *       엄격한 파서는 그걸 거부해 분석 전체를 버린다(#1811)</li>
 * </ul>
 */
final class TolerantJson {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .build();

    private TolerantJson() {}

    static JsonNode readTree(String json) throws java.io.IOException {
        return MAPPER.readTree(json);
    }
}
