package lv.reinis.tinyledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import lv.reinis.tinyledger.dto.AccountDto;
import lv.reinis.tinyledger.dto.CustomerDto;
import lv.reinis.tinyledger.dto.TransactionDto;
import lv.reinis.tinyledger.exception.CustomerException;
import lv.reinis.tinyledger.exception.TransactionException;


@SpringBootTest
@Transactional
class LedgerServiceTests
{

	@Autowired
	private CustomerService customerService;

	@Autowired
	private AccountService accountService;

	@Autowired
	private TransactionService transactionService;

	private UUID customerId;

	@BeforeEach
	void createCustomer()
	{
		customerId = customerService.create("Anna").id();
	}

	@Test
	void createdCustomerCanBeFetched()
	{
		final CustomerDto customer = customerService.get(customerId);

		assertThat(customer.id()).isEqualTo(customerId);
		assertThat(customer.name()).isEqualTo("Anna");
		assertThat(customer.createdAt()).isNotNull();
	}

	@Test
	void newAccountStartsWithZeroBalance()
	{
		final AccountDto account = accountService.openAccount(customerId, "eur");

		assertThat(account.currency()).isEqualTo("EUR");
		assertThat(account.balance()).isEqualByComparingTo("0");
		assertThat(account.id()).isNotNull();
		assertThat(account.customerId()).isEqualTo(customerId);
		assertThat(account.createdAt()).isNotNull();
	}

	@Test
	void customerCanHoldAccountsInDifferentCurrencies()
	{
		accountService.openAccount(customerId, "KWD");
		accountService.openAccount(customerId, "EUR");
		accountService.openAccount(customerId, "JPY");

		assertThat(accountService.listAccounts(customerId)).extracting(AccountDto::currency).containsExactly("EUR", "JPY", "KWD");
	}

	@Test
	void secondAccountInSameCurrencyIsRejected()
	{
		accountService.openAccount(customerId, "EUR");

		assertThatThrownBy(() -> accountService.openAccount(customerId, "EUR")).isInstanceOf(CustomerException.class).hasFieldOrPropertyWithValue("reason", CustomerException.Reason.ACCOUNT_ALREADY_EXISTS);
	}

	@Test
	void unknownCurrencyIsRejected()
	{
		assertThatThrownBy(() -> accountService.openAccount(customerId, "ZZZ")).isInstanceOf(CustomerException.class).hasFieldOrPropertyWithValue("reason", CustomerException.Reason.INVALID_CURRENCY);
	}

	@Test
	void unknownCustomerIsRejected()
	{
		assertThatThrownBy(() -> accountService.openAccount(UUID.randomUUID(), "EUR")).isInstanceOf(CustomerException.class).hasFieldOrPropertyWithValue("reason", CustomerException.Reason.CUSTOMER_NOT_FOUND);
	}

	@Test
	void movementOnMissingAccountIsRejected()
	{
		assertThatThrownBy(() -> transactionService.deposit(customerId, "EUR", new BigDecimal("1"), null))
				.isInstanceOf(CustomerException.class).hasFieldOrPropertyWithValue("reason", CustomerException.Reason.ACCOUNT_NOT_FOUND);
	}

	@Test
	void movementsUpdateBalanceAndRecordBalanceAfter()
	{
		accountService.openAccount(customerId, "EUR");

		final TransactionDto deposit = transactionService.deposit(customerId, "EUR", new BigDecimal("100.50"), "salary");
		final TransactionDto withdrawal = transactionService.withdraw(customerId, "EUR", new BigDecimal("30.25"), "rent");

		assertThat(deposit.id()).isNotNull();
		assertThat(deposit.accountId()).isEqualTo(accountService.getAccount(customerId, "EUR").id());
		assertThat(deposit.createdAt()).isNotNull();
		assertThat(deposit.balanceAfter()).isEqualByComparingTo("100.50");
		assertThat(withdrawal.balanceAfter()).isEqualByComparingTo("70.25");
		assertThat(accountService.getAccount(customerId, "EUR").balance()).isEqualByComparingTo("70.25");
	}

	@Test
	void balancesAreSeparatePerCurrency()
	{
		accountService.openAccount(customerId, "EUR");
		accountService.openAccount(customerId, "JPY");

		transactionService.deposit(customerId, "EUR", new BigDecimal("10"), null);

		assertThat(accountService.getAccount(customerId, "JPY").balance()).isEqualByComparingTo("0");
	}

	@Test
	void withdrawingEntireBalanceIsAllowed()
	{
		accountService.openAccount(customerId, "EUR");
		transactionService.deposit(customerId, "EUR", new BigDecimal("5"), null);

		transactionService.withdraw(customerId, "EUR", new BigDecimal("5"), null);

		assertThat(accountService.getAccount(customerId, "EUR").balance()).isEqualByComparingTo("0");
	}

	@Test
	void overdraftIsRejectedAndBalanceUnchanged()
	{
		accountService.openAccount(customerId, "EUR");
		transactionService.deposit(customerId, "EUR", new BigDecimal("5"), null);

		assertThatThrownBy(() -> transactionService.withdraw(customerId, "EUR", new BigDecimal("5.01"), null))
				.isInstanceOf(TransactionException.class).hasFieldOrPropertyWithValue("reason", TransactionException.Reason.INSUFFICIENT_FUNDS);
		assertThat(accountService.getAccount(customerId, "EUR").balance()).isEqualByComparingTo("5");
		assertThat(transactionService.history(customerId, "EUR")).hasSize(1);
	}

	@Test
	void nonPositiveAmountIsRejected()
	{
		accountService.openAccount(customerId, "EUR");

		assertThatThrownBy(() -> transactionService.deposit(customerId, "EUR", BigDecimal.ZERO, null))
				.isInstanceOf(TransactionException.class).hasFieldOrPropertyWithValue("reason", TransactionException.Reason.INVALID_AMOUNT);
		assertThatThrownBy(() -> transactionService.deposit(customerId, "EUR", new BigDecimal("-1"), null))
				.isInstanceOf(TransactionException.class).hasFieldOrPropertyWithValue("reason", TransactionException.Reason.INVALID_AMOUNT);
	}

	@Test
	void amountPrecisionFollowsCurrency()
	{
		accountService.openAccount(customerId, "JPY");
		accountService.openAccount(customerId, "KWD");

		transactionService.deposit(customerId, "JPY", new BigDecimal("1000"), null);
		transactionService.deposit(customerId, "JPY", new BigDecimal("1000.00"), null);
		transactionService.deposit(customerId, "KWD", new BigDecimal("1.234"), null);

		assertThatThrownBy(() -> transactionService.deposit(customerId, "JPY", new BigDecimal("10.5"), null))
				.isInstanceOf(TransactionException.class).hasFieldOrPropertyWithValue("reason", TransactionException.Reason.INVALID_AMOUNT);
		assertThatThrownBy(() -> transactionService.deposit(customerId, "KWD", new BigDecimal("1.2345"), null))
				.isInstanceOf(TransactionException.class).hasFieldOrPropertyWithValue("reason", TransactionException.Reason.INVALID_AMOUNT);
	}

	@Test
	void historyIsNewestFirst()
	{
		accountService.openAccount(customerId, "EUR");
		transactionService.deposit(customerId, "EUR", new BigDecimal("10"), "first");
		transactionService.deposit(customerId, "EUR", new BigDecimal("20"), "second");
		transactionService.withdraw(customerId, "EUR", new BigDecimal("5"), "third");

		final List<TransactionDto> history = transactionService.history(customerId, "EUR");

		assertThat(history).extracting(TransactionDto::description).containsExactly("third", "second", "first");
	}

}
