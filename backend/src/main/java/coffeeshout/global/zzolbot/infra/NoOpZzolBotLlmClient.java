package coffeeshout.global.zzolbot.infra;

import coffeeshout.global.zzolbot.domain.ZzolBotLlmResponse;
import coffeeshout.global.zzolbot.domain.ZzolBotTool;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local | test")
public class NoOpZzolBotLlmClient implements ZzolBotLlmClient {

    @Override
    public ZzolBotLlmResponse generate(List<Content> conversation, List<ZzolBotTool> tools) {
        return new ZzolBotLlmResponse.TextResponse("ZzolBot이 비활성화된 환경입니다.");
    }

    @Override
    public Content buildFunctionResponseContent(String toolName, String result) {
        return Content.fromParts(Part.fromText(result));
    }
}
