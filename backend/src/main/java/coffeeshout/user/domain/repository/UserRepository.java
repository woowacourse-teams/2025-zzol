package coffeeshout.user.domain.repository;

import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserCode;
import coffeeshout.user.domain.UserNickname;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<User> findAllByNickname(UserNickname nickname);

    Optional<User> findByUserCode(UserCode userCode);
}
