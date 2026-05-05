package coffeeshout.global.zzolbot.infra;

import coffeeshout.global.zzolbot.domain.ZzolBotLlmResponse;
import coffeeshout.global.zzolbot.domain.ZzolBotMessage;
import coffeeshout.global.zzolbot.domain.ZzolBotTool;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local | test")
public class NoOpZzolBotLlmClient implements ZzolBotLlmClient {

    @Override
    public ZzolBotLlmResponse generate(List<ZzolBotMessage> conversation, List<ZzolBotTool> tools, String systemInstruction) {
        return new ZzolBotLlmResponse.TextResponse("ZzolBot이 비활성화된 환경입니다.");
    }
}
