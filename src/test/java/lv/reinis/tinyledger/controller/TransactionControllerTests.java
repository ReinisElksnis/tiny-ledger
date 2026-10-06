package lv.reinis.tinyledger.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import lv.reinis.tinyledger.domain.TransactionType;
import lv.reinis.tinyledger.dto.TransactionDto;
import lv.reinis.tinyledger.exception.CustomerException;
import lv.reinis.tinyledger.exception.TransactionException;
import lv.reinis.tinyledger.service.TransactionService;


@ExtendWith(MockitoExtension.class)
class TransactionControllerTests
{

	private static final UUID CUSTOMER_ID = UUID.randomUUID();

	@Mock
	private TransactionService transactionService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp()
	{
		mockMvc = MockMvcBuilders.standaloneSetup(new TransactionController(transactionService))
				.setControllerAdvice(new GlobalExceptionHandler()).build();
	}

	@Test
	void depositTypeIsRoutedToDeposit() throws Exception
	{
		when(transactionService.deposit(CUSTOMER_ID, "EUR", new BigDecimal("100.50"), "salary"))
				.thenReturn(dto(TransactionType.DEPOSIT, "100.50", "100.50", "salary"));

		create("{\"type\": \"DEPOSIT\", \"amount\": 100.50, \"description\": \"salary\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("DEPOSIT"))
				.andExpect(jsonPath("$.description").value("salary"))
				.andExpect(content().string(containsString("\"amount\":100.50,")))
				.andExpect(content().string(containsString("\"balanceAfter\":100.50,")));
		verifyNoMoreInteractions(transactionService);
	}

	@Test
	void withdrawalTypeIsRoutedToWithdraw() throws Exception
	{
		when(transactionService.withdraw(CUSTOMER_ID, "EUR", new BigDecimal("30.25"), null))
				.thenReturn(dto(TransactionType.WITHDRAWAL, "30.25", "70.25", null));

		create("{\"type\": \"WITHDRAWAL\", \"amount\": 30.25}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("WITHDRAWAL"))
				.andExpect(content().string(containsString("\"balanceAfter\":70.25,")));
		verifyNoMoreInteractions(transactionService);
	}

	@Test
	void missingOrUnknownTypeIsBadRequest() throws Exception
	{
		create("{\"amount\": 5}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.type").isNotEmpty());
		create("{\"type\": null, \"amount\": 5}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.type").isNotEmpty());
		create("{\"type\": \"TRANSFER\", \"amount\": 5}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Validation failed"))
				.andExpect(jsonPath("$.errors.type").value("must be one of DEPOSIT, WITHDRAWAL"));
		create("{\"type\": \"deposit\", \"amount\": 5}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.type").value("must be one of DEPOSIT, WITHDRAWAL"));
		create("{\"type\": \"\", \"amount\": 5}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.type").value("must be one of DEPOSIT, WITHDRAWAL"));

		verifyNoInteractions(transactionService);
	}

	@Test
	void missingOrMalformedAmountIsBadRequest() throws Exception
	{
		create("{\"type\": \"DEPOSIT\"}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.amount").isNotEmpty());
		create("{\"type\": \"DEPOSIT\", \"amount\": \"abc\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.amount").value("has an invalid value"));
		create("{}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.type").isNotEmpty())
				.andExpect(jsonPath("$.errors.amount").isNotEmpty());

		verifyNoInteractions(transactionService);
	}

	@Test
	void tooLongDescriptionIsBadRequest() throws Exception
	{
		create("{\"type\": \"DEPOSIT\", \"amount\": 5, \"description\": \"" + "a".repeat(256) + "\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.description").isNotEmpty());

		verifyNoInteractions(transactionService);
	}

	@Test
	void invalidAmountIsBadRequest() throws Exception
	{
		when(transactionService.deposit(CUSTOMER_ID, "JPY", new BigDecimal("10.5"), null))
				.thenThrow(TransactionException.invalidAmount("JPY amounts allow at most 0 decimal places"));

		mockMvc.perform(post("/api/v1/customers/{id}/accounts/JPY/transactions", CUSTOMER_ID).contentType(MediaType.APPLICATION_JSON)
						.content("{\"type\": \"DEPOSIT\", \"amount\": 10.5}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.reason").value("INVALID_AMOUNT"));
	}

	@Test
	void overdraftIsUnprocessable() throws Exception
	{
		when(transactionService.withdraw(CUSTOMER_ID, "EUR", new BigDecimal("5.01"), null))
				.thenThrow(TransactionException.insufficientFunds("EUR", new BigDecimal("5"), new BigDecimal("5.01")));

		create("{\"type\": \"WITHDRAWAL\", \"amount\": 5.01}")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.reason").value("INSUFFICIENT_FUNDS"));
	}

	@Test
	void movementOnMissingAccountIsNotFound() throws Exception
	{
		when(transactionService.deposit(CUSTOMER_ID, "EUR", new BigDecimal("1"), null))
				.thenThrow(CustomerException.accountNotFound(CUSTOMER_ID, "EUR"));

		create("{\"type\": \"DEPOSIT\", \"amount\": 1}")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.reason").value("ACCOUNT_NOT_FOUND"));
	}

	@Test
	void historyReturnsTransactions() throws Exception
	{
		when(transactionService.history(CUSTOMER_ID, "EUR")).thenReturn(List.of(
				dto(TransactionType.WITHDRAWAL, "30.25", "70.25", "rent"), dto(TransactionType.DEPOSIT, "100.50", "100.50", "salary")));

		mockMvc.perform(get("/api/v1/customers/{id}/accounts/EUR/transactions", CUSTOMER_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].description", contains("rent", "salary")));
		verify(transactionService).history(CUSTOMER_ID, "EUR");
	}

	private ResultActions create(final String body) throws Exception
	{
		return mockMvc.perform(
				post("/api/v1/customers/{id}/accounts/EUR/transactions", CUSTOMER_ID).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private static TransactionDto dto(final TransactionType type, final String amount, final String balanceAfter,
			final String description)
	{
		return new TransactionDto(UUID.randomUUID(), UUID.randomUUID(), type, new BigDecimal(amount), new BigDecimal(balanceAfter),
				description, Instant.now());
	}

}
