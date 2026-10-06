package lv.reinis.tinyledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lv.reinis.tinyledger.converter.Converter;
import lv.reinis.tinyledger.domain.Customer;
import lv.reinis.tinyledger.dto.CustomerDto;
import lv.reinis.tinyledger.exception.CustomerException;
import lv.reinis.tinyledger.repository.CustomerRepository;


@ExtendWith(MockitoExtension.class)
class CustomerServiceTests
{

	private static final UUID CUSTOMER_ID = UUID.randomUUID();

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private Converter<Customer, CustomerDto> customerConverter;

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

}
