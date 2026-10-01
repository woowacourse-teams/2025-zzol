package coffeeshout.admin.auth.ui.request;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
        @NotBlank(message = "ID 토큰은 필수입니다.") String idToken) {}
