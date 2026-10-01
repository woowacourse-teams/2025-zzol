package coffeeshout.admin.account.domain;

import java.util.List;
import java.util.Optional;

public interface AdminAccountRepository {

    AdminAccount save(AdminAccount account);

    Optional<AdminAccount> findById(Long id);

    boolean existsByEmail(String email);

    List<AdminAccount> findAllByOrderByCreatedAtAsc();

    void delete(AdminAccount account);
}
