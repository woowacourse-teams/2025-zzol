package coffeeshout.global.zzolbot.infra;

import coffeeshout.global.zzolbot.domain.AskContext;
import coffeeshout.global.zzolbot.domain.ZzolBotLlmResponse;
import coffeeshout.global.zzolbot.domain.ZzolBotMessage;
import coffeeshout.global.zzolbot.domain.ZzolBotTool;
import java.util.List;

public interface ZzolBotLlmClient {

    ZzolBotLlmResponse generate(List<ZzolBotMessage> conversation, List<ZzolBotTool> tools, String systemInstruction, AskContext ctx);
}
