package coffeeshout.zzolbot.eval.ui.request;

import jakarta.validation.constraints.NotBlank;

/**
 * kind는 실행 대상 시나리오 종류(CHAT/MONITOR). null 또는 빈 값이면 전체를 실행한다.
 */
public record RunRequest(@NotBlank String label, Integer repeats, String kind) {}
