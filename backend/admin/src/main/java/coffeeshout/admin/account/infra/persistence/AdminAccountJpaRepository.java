package coffeeshout.admin.account.infra.persistence;

import coffeeshout.admin.account.domain.AdminAccount;
import coffeeshout.admin.account.domain.AdminAccountRepository;
import org.springframework.data.repository.Repository;

public interface AdminAccountJpaRepository extends Repository<AdminAccount, Long>, AdminAccountRepository {}
