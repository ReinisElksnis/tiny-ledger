package lv.reinis.tinyledger.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lv.reinis.tinyledger.dto.CreateCustomerRequest;
import lv.reinis.tinyledger.dto.CustomerAccountsDto;
import lv.reinis.tinyledger.dto.CustomerDto;
import lv.reinis.tinyledger.service.CustomerService;


@RestController
@Tag(name = "Customers", description = "Create and look up customers.")
@RequestMapping("/api/v1/customers")
public class CustomerController
{

	private CustomerService customerService;

	public CustomerController(final CustomerService customerService)
	{
		this.customerService = customerService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a customer")
	public CustomerDto create(@Valid @RequestBody final CreateCustomerRequest request)
	{
		return customerService.create(request.name());
	}

	@GetMapping
	@Operation(summary = "List all customers with their accounts")
	public List<CustomerAccountsDto> list()
	{
		return customerService.listWithAccounts();
	}

	@GetMapping("/{customerId}")
	@Operation(summary = "Get a customer")
	public CustomerDto get(@PathVariable final UUID customerId)
	{
		return customerService.get(customerId);
	}

}
