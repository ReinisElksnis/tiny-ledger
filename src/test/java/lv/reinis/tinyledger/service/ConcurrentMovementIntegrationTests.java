package lv.reinis.tinyledger.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import lv.reinis.tinyledger.dto.TransactionDto;
import lv.reinis.tinyledger.exception.TransactionException;


// Not @Transactional: every movement has to commit in its own transaction to contend for the account.
@SpringBootTest
class ConcurrentMovementIntegrationTests
{

	private static final int THREADS = 8;

	@Autowired
	private CustomerService customerService;

	@Autowired
	private AccountService accountService;

	@Autowired
	private TransactionService transactionService;

	private UUID customerId;

	@BeforeEach
	void openAccount()
	{
		customerId = customerService.create("Anna").id();
		accountService.openAccount(customerId, "EUR");
	}

	@Test
	void concurrentDepositsAreAllApplied() throws Exception
	{
		final List<Boolean> results = runConcurrently(40,
				() -> transactionService.deposit(customerId, "EUR", new BigDecimal("10"), null));

		assertThat(results).containsOnly(true);
		assertThat(accountService.getAccount(customerId, "EUR").balance()).isEqualByComparingTo("400");
		assertHistoryIsConsistent(40);
	}

	@Test
	void concurrentWithdrawalsCannotOverdraw() throws Exception
	{
		transactionService.deposit(customerId, "EUR", new BigDecimal("100"), null);

		final List<Boolean> results = runConcurrently(40,
				() -> transactionService.withdraw(customerId, "EUR", new BigDecimal("10"), null));

		assertThat(results).filteredOn(Boolean::booleanValue).hasSize(10);
		assertThat(accountService.getAccount(customerId, "EUR").balance()).isEqualByComparingTo("0");
		assertHistoryIsConsistent(11);
	}

	@Test
	void concurrentDepositsAndWithdrawalsKeepBalanceConsistent() throws Exception
	{
		transactionService.deposit(customerId, "EUR", new BigDecimal("200"), null);
		final List<Callable<TransactionDto>> movements = new ArrayList<>();

		for (int i = 0; i < 20; i++)
		{
			movements.add(() -> transactionService.deposit(customerId, "EUR", new BigDecimal("7"), null));
			movements.add(() -> transactionService.withdraw(customerId, "EUR", new BigDecimal("3"), null));
		}

		assertThat(runConcurrently(movements)).containsOnly(true);
		assertThat(accountService.getAccount(customerId, "EUR").balance()).isEqualByComparingTo("280");
		assertHistoryIsConsistent(41);
	}

	// Newest first, so each balanceAfter must follow from the one after it in the list.
	private void assertHistoryIsConsistent(final int expectedSize)
	{
		final List<TransactionDto> history = transactionService.history(customerId, "EUR");
		assertThat(history).hasSize(expectedSize);
		BigDecimal balance = BigDecimal.ZERO;

		for (final TransactionDto transaction : history.reversed())
		{
			balance = switch (transaction.type())
			{
				case DEPOSIT -> balance.add(transaction.amount());
				case WITHDRAWAL -> balance.subtract(transaction.amount());
			};
			assertThat(transaction.balanceAfter()).isEqualByComparingTo(balance);
		}
	}

	private List<Boolean> runConcurrently(final int times, final Callable<TransactionDto> movement) throws Exception
	{
		final List<Callable<TransactionDto>> movements = new ArrayList<>();

		for (int i = 0; i < times; i++)
		{
			movements.add(movement);
		}

		return runConcurrently(movements);
	}

	// Returns, per movement, whether it went through; insufficient funds is the only accepted failure.
	private List<Boolean> runConcurrently(final List<Callable<TransactionDto>> movements) throws Exception
	{
		final CountDownLatch start = new CountDownLatch(1);
		final List<Future<Boolean>> futures = new ArrayList<>();
		final List<Boolean> results = new ArrayList<>();

		try (final ExecutorService executor = Executors.newFixedThreadPool(THREADS))
		{
			for (final Callable<TransactionDto> movement : movements)
			{
				futures.add(executor.submit(() -> {
					start.await();

					try
					{
						movement.call();

						return true;
					}
					catch (final TransactionException e)
					{
						assertThat(e.getReason()).isEqualTo(TransactionException.Reason.INSUFFICIENT_FUNDS);

						return false;
					}
				}));
			}
			start.countDown();

			for (final Future<Boolean> future : futures)
			{
				results.add(future.get(30, TimeUnit.SECONDS));
			}
		}

		return results;
	}

}
