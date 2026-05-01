package coffeeshout.user.domain.repository;

import coffeeshout.user.application.service.AuthTokenService.TokenPair;
import java.util.Optional;

public interface OAuthCodeRepository {

    void save(String code, TokenPair tokens, long ttlSeconds);

    Optional<TokenPair> findAndDelete(String code);
}
