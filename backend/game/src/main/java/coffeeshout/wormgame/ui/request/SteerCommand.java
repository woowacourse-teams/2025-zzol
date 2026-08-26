package coffeeshout.wormgame.ui.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** angle은 라디안 목표각. NaN·무한대는 도메인이 거부한다(INVALID_STEERING). seq는 클라 단조증가 일련번호. */
public record SteerCommand(
        @NotBlank(message = "플레이어 이름은 필수입니다") String playerName,
        double angle,
        @PositiveOrZero long seq) {}
