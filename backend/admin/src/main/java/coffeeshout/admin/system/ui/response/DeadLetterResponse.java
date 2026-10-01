package coffeeshout.admin.system.ui.response;

import coffeeshout.admin.system.domain.DeadLetter;
import coffeeshout.admin.system.domain.DeadLetterSource;
import java.time.Instant;

/**
 * @param requeueable 화면이 "다시 넣기" 버튼을 그릴지 정하는 값. 소스마다 고정이지만
 *                    행마다 실어 보낸다. 프론트가 소스별 규칙표를 따로 들고 있으면
 *                    규칙이 바뀔 때 한쪽만 고치는 날이 온다.
 */
public record DeadLetterResponse(
        DeadLetterSource source,
        Long id,
        String reference,
        String reason,
        String payload,
        Integer retryCount,
        boolean requeueable,
        Instant createdAt) {

    public static DeadLetterResponse from(DeadLetter deadLetter) {
        return new DeadLetterResponse(
                deadLetter.source(),
                deadLetter.id(),
                deadLetter.reference(),
                deadLetter.reason(),
                deadLetter.payload(),
                deadLetter.retryCount(),
                deadLetter.source().isRequeueable(),
                deadLetter.createdAt());
    }
}
