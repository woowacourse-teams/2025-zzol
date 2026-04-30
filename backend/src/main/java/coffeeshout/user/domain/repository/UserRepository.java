package coffeeshout.user.domain.repository;

import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserCode;
import java.util.Optional;

public interface UserRepository {

    boolean existsByUserCode(UserCode userCode);

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByProviderAndProviderUserId(String provider, String providerUserId);
}
