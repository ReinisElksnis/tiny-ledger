package lv.reinis.tinyledger.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import lv.reinis.tinyledger.domain.Account;


public interface AccountRepository extends JpaRepository<Account, UUID>
{

	Optional<Account> findByCustomerIdAndCurrency(final UUID customerId, final String currency);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Account> findForUpdateByCustomerIdAndCurrency(final UUID customerId, final String currency);

	List<Account> findAllByCustomerIdOrderByCurrency(final UUID customerId);

	List<Account> findAllByOrderByCurrency();

	boolean existsByCustomerIdAndCurrency(final UUID customerId, final String currency);

}
