package coffeeshout.user.domain.repository;

import coffeeshout.user.domain.User;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByProviderAndProviderUserId(String provider, String providerUserId);
}
