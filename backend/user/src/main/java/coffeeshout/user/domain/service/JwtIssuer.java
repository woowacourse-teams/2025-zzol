package coffeeshout.user.domain.service;

import coffeeshout.user.domain.AuthenticatedUser;

public interface JwtIssuer {

    String issue(AuthenticatedUser user);

    AuthenticatedUser verify(String token);
}
