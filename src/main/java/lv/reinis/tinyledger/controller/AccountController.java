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

import jakarta.validation.Valid;
import lv.reinis.tinyledger.dto.AccountDto;
import lv.reinis.tinyledger.dto.OpenAccountRequest;
import lv.reinis.tinyledger.service.AccountService;


@RestController
@RequestMapping("/api/v1/customers/{customerId}/accounts")
public class AccountController
{

	private AccountService accountService;

	public AccountController(final AccountService accountService)
	{
		this.accountService = accountService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AccountDto open(@PathVariable final UUID customerId, @Valid @RequestBody final OpenAccountRequest request)
	{
		return accountService.openAccount(customerId, request.currency());
	}

	@GetMapping
	public List<AccountDto> list(@PathVariable final UUID customerId)
	{
		return accountService.listAccounts(customerId);
	}

	@GetMapping("/{currency}")
	public AccountDto get(@PathVariable final UUID customerId, @PathVariable final String currency)
	{
		return accountService.getAccount(customerId, currency);
	}

}
