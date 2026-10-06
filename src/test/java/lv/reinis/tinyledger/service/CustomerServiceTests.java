package lv.reinis.tinyledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import lv.reinis.tinyledger.converter.Converter;
import lv.reinis.tinyledger.converter.CustomerAccountsConverter;
import lv.reinis.tinyledger.domain.Account;
import lv.reinis.tinyledger.domain.Customer;
import lv.reinis.tinyledger.dto.CustomerAccountsDto;
import lv.reinis.tinyledger.dto.CustomerDto;
import lv.reinis.tinyledger.exception.CustomerException;
import lv.reinis.tinyledger.repository.AccountRepository;
import lv.reinis.tinyledger.repository.CustomerRepository;


@ExtendWith(MockitoExtension.class)
class CustomerServiceTests
{

	private static final UUID CUSTOMER_ID = UUID.randomUUID();

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private Converter<Customer, CustomerDto> customerConverter;

	@Mock
	private CustomerAccountsConverter customerAccountsConverter;

	@InjectMocks
	private CustomerService customerService;

	@Test
	void createSavesCustomerAndReturnsDto()
	{
		final CustomerDto dto = new CustomerDto(CUSTOMER_ID, "Anna", Instant.now());
		when(customerRepository.saveAndFlush(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(customerConverter.convert(any(Customer.class))).thenReturn(dto);

		final CustomerDto result = customerService.create("Anna");

		final ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
		verify(customerRepository).saveAndFlush(saved.capture());
		verify(customerConverter).convert(saved.getValue());
		assertThat(saved.getValue().getName()).isEqualTo("Anna");
		assertThat(result).isSameAs(dto);
	}

	@Test
	void getReturnsDtoOfExistingCustomer()
	{
		final Customer customer = new Customer("Anna");
		final CustomerDto dto = new CustomerDto(CUSTOMER_ID, "Anna", Instant.now());
		when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
		when(customerConverter.convert(customer)).thenReturn(dto);

		assertThat(customerService.get(CUSTOMER_ID)).isSameAs(dto);
	}

	@Test
	void getUnknownCustomerIsRejected()
	{
		when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> customerService.get(CUSTOMER_ID)).isInstanceOf(CustomerException.class)
				.hasFieldOrPropertyWithValue("reason", CustomerException.Reason.CUSTOMER_NOT_FOUND);
		verifyNoInteractions(customerConverter);
	}

	@Test
	void listWithAccountsGroupsAccountsByCustomer()
	{
		final Customer anna = customer("Anna");
		final Customer marta = customer("Marta");
		final Account annaEuro = new Account(anna, "EUR");
		final Account annaYen = new Account(anna, "JPY");
		final CustomerAccountsDto annaDto = dto("Anna");
		final CustomerAccountsDto martaDto = dto("Marta");
		when(customerRepository.findAll(Sort.by("createdAt"))).thenReturn(List.of(anna, marta));
		when(accountRepository.findAllByOrderByCurrency()).thenReturn(List.of(annaEuro, annaYen));
		when(customerAccountsConverter.convert(anna, List.of(annaEuro, annaYen))).thenReturn(annaDto);
		when(customerAccountsConverter.convert(marta, List.of())).thenReturn(martaDto);

		assertThat(customerService.listWithAccounts()).containsExactly(annaDto, martaDto);
	}

	private static Customer customer(final String name)
	{
		final Customer customer = new Customer(name);
		ReflectionTestUtils.setField(customer, "id", UUID.randomUUID());

		return customer;
	}

	private static CustomerAccountsDto dto(final String name)
	{
		return new CustomerAccountsDto(UUID.randomUUID(), name, Instant.now(), List.of());
	}

}
