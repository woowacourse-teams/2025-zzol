package coffeeshout.global.zzolbot.ui;

import coffeeshout.global.zzolbot.application.ZzolBotChatService;
import coffeeshout.global.zzolbot.domain.ZzolBotChatResult;
import coffeeshout.global.zzolbot.domain.ZzolBotFeedback;
import coffeeshout.global.zzolbot.infra.ZzolBotSessionEntity;
import java.io.IOException;
import java.security.Principal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.of("Asia/Seoul"));

    private final ZzolBotChatService chatService;

    @GetMapping
    public String page() {
        return "admin/zzolbot";
    }

    @PostMapping("/ask")
    @ResponseBody
    public SseEmitter ask(@RequestBody AskRequest request, Principal principal) {
        final SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        final String adminUsername = principal != null ? principal.getName() : "admin";

        CompletableFuture.runAsync(() -> {
            try {
                final ZzolBotChatResult result = chatService.ask(
                        request.question(),
                        adminUsername,
                        toolName -> {
                            try {
                                emitter.send(SseEmitter.event().name("progress").data(toolName));
                            } catch (IOException e) {
                                log.warn("[ZzolBot] SSE progress 전송 실패. toolName={}", toolName, e);
                            }
                        }
                );
                emitter.send(SseEmitter.event().name("sessionId").data(result.sessionId()));
                emitter.send(SseEmitter.event().name("result").data(result.answer()));
                emitter.complete();
            } catch (Exception e) {
                log.warn("[ZzolBot] SSE 처리 중 오류 발생", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @PostMapping("/sessions/{id}/feedback")
    @ResponseBody
    public ResponseEntity<Void> feedback(
            @PathVariable Long id,
            @RequestBody FeedbackRequest request
    ) {
        chatService.applyFeedback(id, request.feedback());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sessions")
    @ResponseBody
    public List<SessionResponse> sessions() {
        return chatService.getRecentSessions().stream()
                .map(s -> new SessionResponse(
                        s.getId(),
                        s.getQuestion(),
                        s.getAnswer(),
                        s.getFeedback() != null ? s.getFeedback().name() : null,
                        FORMATTER.format(s.getCreatedAt())
                ))
                .toList();
    }

    record AskRequest(String question) {}

    record FeedbackRequest(ZzolBotFeedback feedback) {}

    record SessionResponse(Long id, String question, String answer, String feedback, String createdAt) {}
}
