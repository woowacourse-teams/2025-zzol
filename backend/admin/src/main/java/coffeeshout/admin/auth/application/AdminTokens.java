package coffeeshout.admin.auth.application;

import coffeeshout.admin.auth.domain.AdminRefreshToken;

public record AdminTokens(String accessToken, AdminRefreshToken refreshToken) {}
