package coffeeshout.zzolbot.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 답변이 SSE 로 나갈 때의 바이트 모양을 고정한다.
 *
 * <p>백오피스 프론트가 이 프레이밍을 직접 파싱한다. "{@code data:} 가 여러 줄로 쪼개져
 * 온다"는 가정 위에 파서를 짜 두었는데, 그 가정이 맞는지는 <b>여기서만</b> 확인된다.
 * 프론트 테스트는 자기가 만든 입력을 자기가 파싱하므로 서버가 실제로 그렇게 보내는지는
 * 알 수 없다.
 *
 * <p>리뷰에서 "서버는 {@code data:} 를 한 번만 붙이니 둘째 줄부터 버려진다"는 지적이
 * 있었다. 실제로 재현해 보니 Spring 이 개행마다 {@code data:} 를 붙이고 있었다. 그
 * 사실을 말로만 남기면 다음에 또 같은 의심을 하게 되므로 테스트로 박아 둔다.
 */
@DisplayName("ZzolBot SSE 프레이밍")
class SseFramingTest {

    private static String wireOf(String answer) {
        return SseEmitter.event().name("result").data(answer).build().stream()
                .map(item -> String.valueOf(item.getData()))
                .collect(Collectors.joining());
    }

    @Test
    void 줄바꿈이_있는_답변은_줄마다_data_가_붙는다() {
        assertThat(wireOf("첫 줄\n둘째 줄")).isEqualTo("event:result\ndata:첫 줄\ndata:둘째 줄\n\n");
    }

    @Test
    void 답변_속_빈_줄은_내용이_없는_data_로_나간다() {
        // 여기서 진짜 빈 줄이 나가면 그 자리가 이벤트 끝으로 읽혀, 파서가 앞부분만 답으로
        // 잡고 뒤를 버린다. LLM 답변은 대개 문단이 여럿이라 늘 걸리는 자리다.
        assertThat(wireOf("가\n\n나")).isEqualTo("event:result\ndata:가\ndata:\ndata:나\n\n");
    }

    @Test
    void 이벤트는_빈_줄로_끝난다() {
        // 이 종료 표시가 없으면 파서가 다음 이벤트를 같은 이벤트로 이어 붙인다.
        assertThat(wireOf("한 줄")).endsWith("\n\n");
    }
}
