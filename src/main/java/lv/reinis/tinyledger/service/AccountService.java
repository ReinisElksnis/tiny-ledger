package lv.reinis.tinyledger.service;

import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lv.reinis.tinyledger.converter.Converter;
import lv.reinis.tinyledger.domain.Account;
import lv.reinis.tinyledger.domain.Customer;
import lv.reinis.tinyledger.dto.AccountDto;
import lv.reinis.tinyledger.exception.CustomerException;
import lv.reinis.tinyledger.repository.AccountRepository;


@Service
public class AccountService
{

	private AccountRepository accountRepository;

	private CustomerService customerService;

	private Converter<Account, AccountDto> accountConverter;

	public AccountService(final AccountRepository accountRepository,
			final CustomerService customerService,
			final Converter<Account, AccountDto> accountConverter)
	{
		this.accountRepository = accountRepository;
		this.customerService = customerService;
		this.accountConverter = accountConverter;
	}

	@Transactional
	public AccountDto openAccount(final UUID customerId, final String currency)
	{
		final Customer customer = customerService.findCustomer(customerId);
		final String currencyCode = parseCurrency(currency).getCurrencyCode();

		if (accountRepository.existsByCustomerIdAndCurrency(customerId, currencyCode))
		{
			throw CustomerException.accountAlreadyExists(customerId, currencyCode);
		}

		// Flush so the generated timestamps are populated before mapping.
		return accountConverter.convert(accountRepository.saveAndFlush(new Account(customer, currencyCode)));
	}

	@Transactional(readOnly = true)
	public List<AccountDto> listAccounts(final UUID customerId)
	{
		customerService.findCustomer(customerId);

		return accountConverter.convertAll(accountRepository.findAllByCustomerIdOrderByCurrency(customerId));
	}

	@Transactional(readOnly = true)
	public AccountDto getAccount(final UUID customerId, final String currency)
	{
		return accountConverter.convert(findAccount(customerId, currency));
	}

	private Account findAccount(final UUID customerId, final String currency)
	{
		customerService.findCustomer(customerId);
		final String currencyCode = parseCurrency(currency).getCurrencyCode();

		return accountRepository.findByCustomerIdAndCurrency(customerId, currencyCode)
				.orElseThrow(() -> CustomerException.accountNotFound(customerId, currencyCode));
	}

	static Currency parseCurrency(final String currency)
	{
		try
		{
			return Currency.getInstance(currency.toUpperCase(Locale.ROOT));
		}
		catch (final IllegalArgumentException | NullPointerException e)
		{
			throw CustomerException.invalidCurrency(currency);
		}
	}

}
