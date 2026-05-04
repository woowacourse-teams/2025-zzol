package coffeeshout.global.zzolbot.ui;

import coffeeshout.global.zzolbot.application.ZzolBotChatService;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Controller
@RequestMapping("/admin/zzolbot")
@RequiredArgsConstructor
public class ZzolBotChatController {

    private static final long SSE_TIMEOUT_MS = 120_000L;

    private final ZzolBotChatService chatService;

    @GetMapping
    public String page() {
        return "admin/zzolbot";
    }

    @PostMapping("/ask")
    @ResponseBody
    public SseEmitter ask(@RequestBody AskRequest request) {
        final SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        CompletableFuture.runAsync(() -> {
            try {
                final String result = chatService.ask(request.question(), toolName -> {
                    try {
                        emitter.send(SseEmitter.event().name("progress").data(toolName));
                    } catch (IOException e) {
                        log.warn("[ZzolBot] SSE progress 전송 실패. toolName={}", toolName, e);
                    }
                });
                emitter.send(SseEmitter.event().name("result").data(result));
                emitter.complete();
            } catch (Exception e) {
                log.warn("[ZzolBot] SSE 처리 중 오류 발생", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    record AskRequest(String question) {}
}
