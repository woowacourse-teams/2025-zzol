package coffeeshout.zzolbot.ui;

import coffeeshout.zzolbot.application.ZzolBotChatService;
import coffeeshout.zzolbot.domain.ZzolBotChatResult;
import coffeeshout.zzolbot.domain.ZzolBotFeedback;
import coffeeshout.zzolbot.infra.ZzolBotSessionEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.security.Principal;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Controller
@Validated
@RequestMapping("/admin/zzolbot")
public class ZzolBotChatController {

    private static final long SSE_TIMEOUT_MS = 120_000L;

    private final ZzolBotChatService chatService;
    private final ExecutorService virtualThreadExecutor;
    private final DateTimeFormatter formatter;

    public ZzolBotChatController(
            ZzolBotChatService chatService,
            @Qualifier("virtualThreadExecutor") ExecutorService virtualThreadExecutor,
            Clock clock
    ) {
        this.chatService = chatService;
        this.virtualThreadExecutor = virtualThreadExecutor;
        this.formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(clock.getZone());
    }

    @GetMapping
    public String page() {
        return "admin/zzolbot";
    }

    @PostMapping("/ask")
    @ResponseBody
    public SseEmitter ask(@RequestBody @Valid AskRequest request, Principal principal) {
        final SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onTimeout(() -> {
            log.warn("[ZzolBot] SSE 타임아웃 발생");
            emitter.complete();
        });

        if (principal == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        final String adminUsername = principal.getName();

        try {
            virtualThreadExecutor.execute(() -> {
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
        } catch (RejectedExecutionException e) {
            log.warn("[ZzolBot] 가상 스레드 실행 거부", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @PostMapping("/sessions/{id}/feedback")
    @ResponseBody
    public ResponseEntity<Void> feedback(
            @PathVariable Long id,
            @RequestBody @Valid FeedbackRequest request
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
                        formatter.format(s.getCreatedAt())
                ))
                .toList();
    }

    record AskRequest(@NotBlank String question) {}

    record FeedbackRequest(@NotNull ZzolBotFeedback feedback) {}

    record SessionResponse(Long id, String question, String answer, String feedback, String createdAt) {}
}
