package coffeeshout.zzolbot.application;

import coffeeshout.zzolbot.domain.AskContext;
import coffeeshout.zzolbot.domain.ZzolBotChatResult;

/**
 * 진단 결과의 저장 책임을 추상화한다.
 * 운영에서는 {@code zzolbot_session}에 영속화하고,
 * 평가에서는 세션 테이블을 오염시키지 않도록 영속화 없이 결과만 반환한다.
 *
 * <p>전달되는 질문/답변은 이미 PII 마스킹이 적용된 상태다.
 */
@FunctionalInterface
public interface SessionSink {

    ZzolBotChatResult save(String maskedQuestion, String maskedAnswer, String adminUsername, AskContext ctx);
}
