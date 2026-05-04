package coffeeshout.global.zzolbot.infra;

import coffeeshout.global.zzolbot.domain.ZzolBotLlmResponse;
import coffeeshout.global.zzolbot.domain.ZzolBotTool;
import com.google.genai.types.Content;
import java.util.List;

public interface ZzolBotLlmClient {

    ZzolBotLlmResponse generate(List<Content> conversation, List<ZzolBotTool> tools);

    Content buildFunctionResponseContent(String toolName, String result);
}
