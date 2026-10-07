package lv.reinis.tinyledger.service;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lv.reinis.tinyledger.converter.Converter;
import lv.reinis.tinyledger.domain.Account;
import lv.reinis.tinyledger.domain.Transaction;
import lv.reinis.tinyledger.domain.TransactionType;
import lv.reinis.tinyledger.dto.TransactionDto;
import lv.reinis.tinyledger.exception.CustomerException;
import lv.reinis.tinyledger.exception.TransactionException;
import lv.reinis.tinyledger.repository.AccountRepository;
import lv.reinis.tinyledger.repository.TransactionRepository;
import lv.reinis.tinyledger.util.CurrencyUtils;


@Service
public class TransactionService
{

	private TransactionRepository transactionRepository;

	private AccountRepository accountRepository;

	private CustomerService customerService;

	private Converter<Transaction, TransactionDto> transactionConverter;

	public TransactionService(final TransactionRepository transactionRepository,
			final AccountRepository accountRepository,
			final CustomerService customerService,
			final Converter<Transaction, TransactionDto> transactionConverter)
	{
		this.transactionRepository = transactionRepository;
		this.accountRepository = accountRepository;
		this.customerService = customerService;
		this.transactionConverter = transactionConverter;
	}

	@Transactional
	public TransactionDto deposit(final UUID customerId, final String currency, final BigDecimal amount, final String description)
	{
		final Account account = findAccountForUpdate(customerId, currency);
		validateAmount(amount, Currency.getInstance(account.getCurrency()));

		final BigDecimal balanceAfter = account.getBalance().add(amount);

		return record(account, TransactionType.DEPOSIT, amount, balanceAfter, description);
	}

	@Transactional
	public TransactionDto withdraw(final UUID customerId, final String currency, final BigDecimal amount,
			final String description)
	{
		final Account account = findAccountForUpdate(customerId, currency);
		final Currency accountCurrency = Currency.getInstance(account.getCurrency());
		validateAmount(amount, accountCurrency);

		final BigDecimal balance = account.getBalance();

		if (amount.compareTo(balance) > 0)
		{
			throw TransactionException.insufficientFunds(account.getCurrency(), balance, amount);
		}

		return record(account, TransactionType.WITHDRAWAL, amount, balance.subtract(amount), description);
	}

	@Transactional(readOnly = true)
	public List<TransactionDto> history(final UUID customerId, final String currency)
	{
		final Account account = findAccount(customerId, currency);

		return transactionConverter.convertAll(transactionRepository.findAllByAccountIdOrderByIdDesc(account.getId()));
	}

	private Account findAccount(final UUID customerId, final String currency)
	{
		final String currencyCode = CurrencyUtils.parseCurrency(currency).getCurrencyCode();

		return accountRepository.findByCustomerIdAndCurrency(customerId, currencyCode)
				.orElseThrow(() -> CustomerException.accountNotFound(customerId, currencyCode));
	}

	private Account findAccountForUpdate(final UUID customerId, final String currency)
	{
		final String currencyCode = CurrencyUtils.parseCurrency(currency).getCurrencyCode();

		return accountRepository.findForUpdateByCustomerIdAndCurrency(customerId, currencyCode)
				.orElseThrow(() -> CustomerException.accountNotFound(customerId, currencyCode));
	}

	private TransactionDto record(final Account account, final TransactionType type, final BigDecimal amount,
			final BigDecimal balanceAfter, final String description)
	{
		account.setBalance(balanceAfter);

		// Flush so the generated timestamp is populated before mapping.
		return transactionConverter.convert(
				transactionRepository.saveAndFlush(new Transaction(account, type, amount, balanceAfter, description)));
	}

	private static void validateAmount(final BigDecimal amount, final Currency currency)
	{
		if (amount == null || amount.signum() <= 0)
		{
			throw TransactionException.invalidAmount("Amount must be greater than zero");
		}
		final int fractionDigits = CurrencyUtils.fractionDigits(currency);

		if (amount.stripTrailingZeros().scale() > fractionDigits)
		{
			throw TransactionException.invalidAmount(
					"%s amounts allow at most %d decimal places".formatted(currency, fractionDigits));
		}
	}

}
