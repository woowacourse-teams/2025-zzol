package coffeeshout.admin.account.ui.request;

import jakarta.validation.constraints.NotBlank;

public record AddAdminAccountRequest(
        @NotBlank(message = "이메일은 필수입니다.") String email) {}
