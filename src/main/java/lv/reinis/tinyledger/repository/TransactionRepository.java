package lv.reinis.tinyledger.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import lv.reinis.tinyledger.domain.Transaction;


public interface TransactionRepository extends JpaRepository<Transaction, UUID>
{

	// Ids are time-ordered UUIDs, so this is newest first.
	List<Transaction> findAllByAccountIdOrderByIdDesc(final UUID accountId);

}
