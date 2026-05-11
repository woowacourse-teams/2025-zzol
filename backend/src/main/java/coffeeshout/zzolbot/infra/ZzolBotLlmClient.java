package coffeeshout.zzolbot.infra;

import coffeeshout.zzolbot.domain.AskContext;
import coffeeshout.zzolbot.domain.ZzolBotLlmResponse;
import coffeeshout.zzolbot.domain.ZzolBotMessage;
import coffeeshout.zzolbot.domain.ZzolBotTool;
import java.util.List;

public interface ZzolBotLlmClient {

    ZzolBotLlmResponse generate(List<ZzolBotMessage> conversation, List<ZzolBotTool> tools, String systemInstruction, AskContext ctx);
}
