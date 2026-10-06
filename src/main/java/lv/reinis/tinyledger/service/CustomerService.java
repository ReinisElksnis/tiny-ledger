package lv.reinis.tinyledger.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lv.reinis.tinyledger.converter.Converter;
import lv.reinis.tinyledger.domain.Customer;
import lv.reinis.tinyledger.dto.CustomerDto;
import lv.reinis.tinyledger.exception.CustomerException;
import lv.reinis.tinyledger.repository.CustomerRepository;


@Service
@RequiredArgsConstructor
public class CustomerService
{

	private final CustomerRepository customerRepository;

	private final Converter<Customer, CustomerDto> customerConverter;

	@Transactional
	public CustomerDto create(final String name)
	{
		// Flush so the generated timestamps are populated before mapping.
		return customerConverter.convert(customerRepository.saveAndFlush(new Customer(name)));
	}

	@Transactional(readOnly = true)
	public CustomerDto get(final UUID customerId)
	{
		return customerConverter.convert(findCustomer(customerId));
	}

	// Entity lookup for the other services; entities never leave the service layer.
	Customer findCustomer(final UUID customerId)
	{
		return customerRepository.findById(customerId).orElseThrow(() -> CustomerException.customerNotFound(customerId));
	}

}
