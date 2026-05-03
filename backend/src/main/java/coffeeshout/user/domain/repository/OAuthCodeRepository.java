package coffeeshout.user.domain.repository;

import coffeeshout.user.domain.TokenPair;
import java.util.Optional;

public interface OAuthCodeRepository {

    void save(String code, TokenPair tokens, long ttlSeconds);

    Optional<TokenPair> findAndDelete(String code);
}
