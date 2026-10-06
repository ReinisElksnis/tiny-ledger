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
import lv.reinis.tinyledger.dto.TransactionDto;
import lv.reinis.tinyledger.dto.TransactionRequest;
import lv.reinis.tinyledger.service.TransactionService;


@RestController
@Tag(name = "Transactions", description = "Deposits, withdrawals and transaction history of an account.")
@RequestMapping("/api/v1/customers/{customerId}/accounts/{currency}/transactions")
public class TransactionController
{

	private TransactionService transactionService;

	public TransactionController(final TransactionService transactionService)
	{
		this.transactionService = transactionService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Deposit to or withdraw from an account")
	public TransactionDto create(@PathVariable final UUID customerId, @PathVariable final String currency,
			@Valid @RequestBody final TransactionRequest request)
	{
		return switch (request.type())
		{
			case DEPOSIT -> transactionService.deposit(customerId, currency, request.amount(), request.description());
			case WITHDRAWAL -> transactionService.withdraw(customerId, currency, request.amount(), request.description());
		};
	}

	@GetMapping
	@Operation(summary = "List transactions, newest first")
	public List<TransactionDto> history(@PathVariable final UUID customerId, @PathVariable final String currency)
	{
		return transactionService.history(customerId, currency);
	}

}
