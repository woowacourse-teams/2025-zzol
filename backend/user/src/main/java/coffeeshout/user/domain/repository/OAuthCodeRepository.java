package coffeeshout.user.domain.repository;

import coffeeshout.user.domain.OAuthCodeEntry;
import coffeeshout.user.domain.TokenPair;
import java.util.Optional;

public interface OAuthCodeRepository {

    void save(String code, TokenPair tokens, boolean isNewUser, long ttlSeconds);

    Optional<OAuthCodeEntry> findAndDelete(String code);
}
