package lv.reinis.tinyledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

import lv.reinis.tinyledger.converter.Converter;
import lv.reinis.tinyledger.domain.Account;
import lv.reinis.tinyledger.domain.Customer;
import lv.reinis.tinyledger.dto.AccountDto;
import lv.reinis.tinyledger.exception.CustomerException;
import lv.reinis.tinyledger.repository.AccountRepository;


@ExtendWith(MockitoExtension.class)
class AccountServiceTests
{

	private static final UUID CUSTOMER_ID = UUID.randomUUID();

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private CustomerService customerService;

	@Mock
	private Converter<Account, AccountDto> accountConverter;

	@InjectMocks
	private AccountService accountService;

	private final Customer customer = new Customer("Anna");

	@Test
	void openAccountSavesZeroBalanceAccountInUpperCaseCurrency()
	{
		final AccountDto dto = dto("EUR");
		when(customerService.findCustomer(CUSTOMER_ID)).thenReturn(customer);
		when(accountRepository.existsByCustomerIdAndCurrency(CUSTOMER_ID, "EUR")).thenReturn(false);
		when(accountRepository.saveAndFlush(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(accountConverter.convert(any(Account.class))).thenReturn(dto);

		final AccountDto result = accountService.openAccount(CUSTOMER_ID, "eur");

		final ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
		verify(accountRepository).saveAndFlush(saved.capture());
		assertThat(saved.getValue().getCustomer()).isSameAs(customer);
		assertThat(saved.getValue().getCurrency()).isEqualTo("EUR");
		assertThat(saved.getValue().getBalance()).isEqualByComparingTo("0");
		assertThat(result).isSameAs(dto);
	}

	@Test
	void secondAccountInSameCurrencyIsRejected()
	{
		when(accountRepository.existsByCustomerIdAndCurrency(CUSTOMER_ID, "EUR")).thenReturn(true);

		assertThatThrownBy(() -> accountService.openAccount(CUSTOMER_ID, "EUR")).isInstanceOf(CustomerException.class)
				.hasFieldOrPropertyWithValue("reason", CustomerException.Reason.ACCOUNT_ALREADY_EXISTS);
		verify(accountRepository, never()).saveAndFlush(any(Account.class));
	}

	@Test
	void unknownOrMissingCurrencyIsRejected()
	{
		assertThatThrownBy(() -> accountService.openAccount(CUSTOMER_ID, "ZZZ")).isInstanceOf(CustomerException.class)
				.hasFieldOrPropertyWithValue("reason", CustomerException.Reason.INVALID_CURRENCY);
		assertThatThrownBy(() -> accountService.openAccount(CUSTOMER_ID, null)).isInstanceOf(CustomerException.class)
				.hasFieldOrPropertyWithValue("reason", CustomerException.Reason.INVALID_CURRENCY);
		verifyNoInteractions(accountRepository);
	}

	@Test
	void unknownCustomerIsRejected()
	{
		when(customerService.findCustomer(CUSTOMER_ID)).thenThrow(CustomerException.customerNotFound(CUSTOMER_ID));

		assertThatThrownBy(() -> accountService.openAccount(CUSTOMER_ID, "EUR")).isInstanceOf(CustomerException.class)
				.hasFieldOrPropertyWithValue("reason", CustomerException.Reason.CUSTOMER_NOT_FOUND);
		assertThatThrownBy(() -> accountService.listAccounts(CUSTOMER_ID)).isInstanceOf(CustomerException.class)
				.hasFieldOrPropertyWithValue("reason", CustomerException.Reason.CUSTOMER_NOT_FOUND);
		assertThatThrownBy(() -> accountService.getAccount(CUSTOMER_ID, "EUR")).isInstanceOf(CustomerException.class)
				.hasFieldOrPropertyWithValue("reason", CustomerException.Reason.CUSTOMER_NOT_FOUND);
		verifyNoInteractions(accountRepository);
	}

	@Test
	void listAccountsReturnsDtosOfCustomerAccounts()
	{
		final List<Account> accounts = List.of(new Account(customer, "EUR"), new Account(customer, "JPY"));
		final List<AccountDto> dtos = List.of(dto("EUR"), dto("JPY"));
		when(accountRepository.findAllByCustomerIdOrderByCurrency(CUSTOMER_ID)).thenReturn(accounts);
		when(accountConverter.convertAll(accounts)).thenReturn(dtos);

		assertThat(accountService.listAccounts(CUSTOMER_ID)).isSameAs(dtos);
		verify(customerService).findCustomer(CUSTOMER_ID);
	}

	@Test
	void getAccountLooksUpByUpperCaseCurrency()
	{
		final Account account = new Account(customer, "EUR");
		final AccountDto dto = dto("EUR");
		when(accountRepository.findByCustomerIdAndCurrency(CUSTOMER_ID, "EUR")).thenReturn(Optional.of(account));
		when(accountConverter.convert(account)).thenReturn(dto);

		assertThat(accountService.getAccount(CUSTOMER_ID, "eur")).isSameAs(dto);
	}

	@Test
	void missingAccountIsRejected()
	{
		when(accountRepository.findByCustomerIdAndCurrency(CUSTOMER_ID, "EUR")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> accountService.getAccount(CUSTOMER_ID, "EUR")).isInstanceOf(CustomerException.class)
				.hasFieldOrPropertyWithValue("reason", CustomerException.Reason.ACCOUNT_NOT_FOUND);
		verifyNoInteractions(accountConverter);
	}

	private static AccountDto dto(final String currency)
	{
		return new AccountDto(UUID.randomUUID(), CUSTOMER_ID, currency, BigDecimal.ZERO, Instant.now());
	}

}
