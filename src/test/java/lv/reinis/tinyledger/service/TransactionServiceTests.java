package lv.reinis.tinyledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import lv.reinis.tinyledger.converter.Converter;
import lv.reinis.tinyledger.domain.Account;
import lv.reinis.tinyledger.domain.Customer;
import lv.reinis.tinyledger.domain.Transaction;
import lv.reinis.tinyledger.domain.TransactionType;
import lv.reinis.tinyledger.dto.TransactionDto;
import lv.reinis.tinyledger.exception.CustomerException;
import lv.reinis.tinyledger.exception.TransactionException;
import lv.reinis.tinyledger.repository.AccountRepository;
import lv.reinis.tinyledger.repository.TransactionRepository;


@ExtendWith(MockitoExtension.class)
class TransactionServiceTests
{

	private static final UUID CUSTOMER_ID = UUID.randomUUID();

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private CustomerService customerService;

	@Mock
	private Converter<Transaction, TransactionDto> transactionConverter;

	@InjectMocks
	private TransactionService transactionService;

	@Test
	void depositAddsToBalanceAndRecordsTransaction()
	{
		final Account account = givenAccount("EUR", "100.50");
		final TransactionDto dto = givenTransactionIsSaved();

		final TransactionDto result = transactionService.deposit(CUSTOMER_ID, "eur", new BigDecimal("30.25"), "salary");

		final Transaction saved = savedTransaction();
		assertThat(saved.getAccount()).isSameAs(account);
		assertThat(saved.getType()).isEqualTo(TransactionType.DEPOSIT);
		assertThat(saved.getAmount()).isEqualByComparingTo("30.25");
		assertThat(saved.getBalanceAfter()).isEqualByComparingTo("130.75");
		assertThat(saved.getDescription()).isEqualTo("salary");
		assertThat(account.getBalance()).isEqualByComparingTo("130.75");
		assertThat(result).isSameAs(dto);
	}

	@Test
	void withdrawalSubtractsFromBalanceAndRecordsTransaction()
	{
		final Account account = givenAccount("EUR", "100.50");
		final TransactionDto dto = givenTransactionIsSaved();

		final TransactionDto result = transactionService.withdraw(CUSTOMER_ID, "EUR", new BigDecimal("30.25"), "rent");

		final Transaction saved = savedTransaction();
		assertThat(saved.getType()).isEqualTo(TransactionType.WITHDRAWAL);
		assertThat(saved.getAmount()).isEqualByComparingTo("30.25");
		assertThat(saved.getBalanceAfter()).isEqualByComparingTo("70.25");
		assertThat(account.getBalance()).isEqualByComparingTo("70.25");
		assertThat(result).isSameAs(dto);
	}

	@Test
	void withdrawingEntireBalanceIsAllowed()
	{
		final Account account = givenAccount("EUR", "5");
		givenTransactionIsSaved();

		transactionService.withdraw(CUSTOMER_ID, "EUR", new BigDecimal("5"), null);

		assertThat(account.getBalance()).isEqualByComparingTo("0");
	}

	@Test
	void overdraftIsRejectedAndBalanceUnchanged()
	{
		final Account account = givenAccount("EUR", "5");

		assertThatThrownBy(() -> transactionService.withdraw(CUSTOMER_ID, "EUR", new BigDecimal("5.01"), null))
				.isInstanceOf(TransactionException.class)
				.hasFieldOrPropertyWithValue("reason", TransactionException.Reason.INSUFFICIENT_FUNDS);
		assertThat(account.getBalance()).isEqualByComparingTo("5");
		verifyNoInteractions(transactionRepository);
	}

	@Test
	void missingOrNonPositiveAmountIsRejected()
	{
		final Account account = givenAccount("EUR", "5");

		assertInvalidAmount(() -> transactionService.deposit(CUSTOMER_ID, "EUR", null, null));
		assertInvalidAmount(() -> transactionService.deposit(CUSTOMER_ID, "EUR", BigDecimal.ZERO, null));
		assertInvalidAmount(() -> transactionService.deposit(CUSTOMER_ID, "EUR", new BigDecimal("-1"), null));
		assertInvalidAmount(() -> transactionService.withdraw(CUSTOMER_ID, "EUR", BigDecimal.ZERO, null));
		assertThat(account.getBalance()).isEqualByComparingTo("5");
		verifyNoInteractions(transactionRepository);
	}

	@Test
	void amountWithMoreDecimalsThanCurrencyAllowsIsRejected()
	{
		givenAccount("EUR", "100");
		givenAccount("JPY", "100");
		givenAccount("KWD", "100");

		assertInvalidAmount(() -> transactionService.deposit(CUSTOMER_ID, "EUR", new BigDecimal("1.234"), null));
		assertInvalidAmount(() -> transactionService.deposit(CUSTOMER_ID, "JPY", new BigDecimal("10.5"), null));
		assertInvalidAmount(() -> transactionService.withdraw(CUSTOMER_ID, "JPY", new BigDecimal("0.5"), null));
		assertInvalidAmount(() -> transactionService.deposit(CUSTOMER_ID, "KWD", new BigDecimal("1.2345"), null));
		verifyNoInteractions(transactionRepository);
	}

	@Test
	void amountWithinCurrencyDecimalsIsAccepted()
	{
		givenAccount("JPY", "0");
		givenAccount("KWD", "0");
		givenTransactionIsSaved();

		transactionService.deposit(CUSTOMER_ID, "JPY", new BigDecimal("1000"), null);
		transactionService.deposit(CUSTOMER_ID, "JPY", new BigDecimal("1000.00"), null);
		transactionService.deposit(CUSTOMER_ID, "KWD", new BigDecimal("1.234"), null);

		verify(transactionRepository, times(3)).saveAndFlush(any(Transaction.class));
	}

	@Test
	void movementOnMissingAccountIsRejected()
	{
		when(accountRepository.findByCustomerIdAndCurrency(CUSTOMER_ID, "EUR")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> transactionService.deposit(CUSTOMER_ID, "EUR", new BigDecimal("1"), null))
				.isInstanceOf(CustomerException.class)
				.hasFieldOrPropertyWithValue("reason", CustomerException.Reason.ACCOUNT_NOT_FOUND);
		verify(transactionRepository, never()).saveAndFlush(any(Transaction.class));
	}

	@Test
	void movementForUnknownCustomerIsRejected()
	{
		when(customerService.findCustomer(CUSTOMER_ID)).thenThrow(CustomerException.customerNotFound(CUSTOMER_ID));

		assertThatThrownBy(() -> transactionService.withdraw(CUSTOMER_ID, "EUR", new BigDecimal("1"), null))
				.isInstanceOf(CustomerException.class)
				.hasFieldOrPropertyWithValue("reason", CustomerException.Reason.CUSTOMER_NOT_FOUND);
		verifyNoInteractions(accountRepository, transactionRepository);
	}

	@Test
	void historyReturnsDtosOfAccountTransactions()
	{
		final Account account = givenAccount("EUR", "10");
		final UUID accountId = UUID.randomUUID();
		ReflectionTestUtils.setField(account, "id", accountId);
		final List<Transaction> transactions = List
				.of(new Transaction(account, TransactionType.DEPOSIT, BigDecimal.TEN, BigDecimal.TEN, "first"));
		final List<TransactionDto> dtos = List.of(dto());
		when(transactionRepository.findAllByAccountIdOrderByIdDesc(accountId)).thenReturn(transactions);
		when(transactionConverter.convertAll(transactions)).thenReturn(dtos);

		assertThat(transactionService.history(CUSTOMER_ID, "eur")).isSameAs(dtos);
	}

	private Account givenAccount(final String currency, final String balance)
	{
		final Account account = new Account(new Customer("Anna"), currency);
		account.setBalance(new BigDecimal(balance));
		when(accountRepository.findByCustomerIdAndCurrency(CUSTOMER_ID, currency)).thenReturn(Optional.of(account));

		return account;
	}

	private TransactionDto givenTransactionIsSaved()
	{
		final TransactionDto dto = dto();
		when(transactionRepository.saveAndFlush(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(transactionConverter.convert(any(Transaction.class))).thenReturn(dto);

		return dto;
	}

	private Transaction savedTransaction()
	{
		final ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository).saveAndFlush(saved.capture());

		return saved.getValue();
	}

	private static void assertInvalidAmount(final Runnable movement)
	{
		assertThatThrownBy(movement::run).isInstanceOf(TransactionException.class)
				.hasFieldOrPropertyWithValue("reason", TransactionException.Reason.INVALID_AMOUNT);
	}

	private static TransactionDto dto()
	{
		return new TransactionDto(UUID.randomUUID(), UUID.randomUUID(), TransactionType.DEPOSIT, BigDecimal.TEN, BigDecimal.TEN, null,
				Instant.now());
	}

}
