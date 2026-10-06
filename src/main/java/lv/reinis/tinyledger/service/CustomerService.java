package lv.reinis.tinyledger.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lv.reinis.tinyledger.converter.Converter;
import lv.reinis.tinyledger.converter.CustomerAccountsConverter;
import lv.reinis.tinyledger.domain.Account;
import lv.reinis.tinyledger.domain.Customer;
import lv.reinis.tinyledger.dto.CustomerAccountsDto;
import lv.reinis.tinyledger.dto.CustomerDto;
import lv.reinis.tinyledger.exception.CustomerException;
import lv.reinis.tinyledger.repository.AccountRepository;
import lv.reinis.tinyledger.repository.CustomerRepository;


@Service
public class CustomerService
{

	private CustomerRepository customerRepository;

	private AccountRepository accountRepository;

	private Converter<Customer, CustomerDto> customerConverter;

	private CustomerAccountsConverter customerAccountsConverter;

	public CustomerService(final CustomerRepository customerRepository,
			final AccountRepository accountRepository,
			final Converter<Customer, CustomerDto> customerConverter,
			final CustomerAccountsConverter customerAccountsConverter)
	{
		this.customerRepository = customerRepository;
		this.accountRepository = accountRepository;
		this.customerConverter = customerConverter;
		this.customerAccountsConverter = customerAccountsConverter;
	}

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

	@Transactional(readOnly = true)
	public List<CustomerAccountsDto> listWithAccounts()
	{
		final Map<UUID, List<Account>> accountsByCustomerId = accountRepository.findAllByOrderByCurrency().stream()
				.collect(Collectors.groupingBy(account -> account.getCustomer().getId()));

		return customerRepository.findAll(Sort.by("createdAt")).stream()
				.map(customer -> customerAccountsConverter.convert(customer,
						accountsByCustomerId.getOrDefault(customer.getId(), List.of())))
				.toList();
	}

	public Customer findCustomer(final UUID customerId)
	{
		return customerRepository.findById(customerId).orElseThrow(() -> CustomerException.customerNotFound(customerId));
	}

}
