package lv.reinis.tinyledger.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
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

import lv.reinis.tinyledger.dto.AccountDto;
import lv.reinis.tinyledger.exception.CustomerException;
import lv.reinis.tinyledger.service.AccountService;


@ExtendWith(MockitoExtension.class)
class AccountControllerTests
{

	private static final UUID CUSTOMER_ID = UUID.randomUUID();

	@Mock
	private AccountService accountService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp()
	{
		mockMvc = MockMvcBuilders.standaloneSetup(new AccountController(accountService))
				.setControllerAdvice(new LedgerExceptionHandler()).build();
	}

	@Test
	void openReturnsCreatedAccount() throws Exception
	{
		when(accountService.openAccount(CUSTOMER_ID, "eur")).thenReturn(dto("EUR", "0.00"));

		open("{\"currency\": \"eur\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()))
				.andExpect(jsonPath("$.currency").value("EUR"))
				.andExpect(content().string(containsString("\"balance\":0.00,")));
	}

	@Test
	void missingCurrencyIsBadRequest() throws Exception
	{
		open("{}").andExpect(status().isBadRequest());
		open("{\"currency\": \"\"}").andExpect(status().isBadRequest());

		verifyNoInteractions(accountService);
	}

	@Test
	void duplicateAccountIsConflict() throws Exception
	{
		when(accountService.openAccount(CUSTOMER_ID, "EUR")).thenThrow(CustomerException.accountAlreadyExists(CUSTOMER_ID, "EUR"));

		open("{\"currency\": \"EUR\"}").andExpect(status().isConflict()).andExpect(jsonPath("$.reason").value("ACCOUNT_ALREADY_EXISTS"));
	}

	@Test
	void invalidCurrencyIsBadRequest() throws Exception
	{
		when(accountService.openAccount(CUSTOMER_ID, "ZZZ")).thenThrow(CustomerException.invalidCurrency("ZZZ"));

		open("{\"currency\": \"ZZZ\"}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.reason").value("INVALID_CURRENCY"));
	}

	@Test
	void unknownCustomerIsNotFound() throws Exception
	{
		when(accountService.listAccounts(CUSTOMER_ID)).thenThrow(CustomerException.customerNotFound(CUSTOMER_ID));

		mockMvc.perform(get("/api/v1/customers/{id}/accounts", CUSTOMER_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.reason").value("CUSTOMER_NOT_FOUND"));
	}

	@Test
	void listReturnsAccounts() throws Exception
	{
		when(accountService.listAccounts(CUSTOMER_ID)).thenReturn(List.of(dto("EUR", "100.50"), dto("JPY", "1000")));

		mockMvc.perform(get("/api/v1/customers/{id}/accounts", CUSTOMER_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].currency", contains("EUR", "JPY")));
	}

	@Test
	void getReturnsAccount() throws Exception
	{
		when(accountService.getAccount(CUSTOMER_ID, "EUR")).thenReturn(dto("EUR", "100.50"));

		mockMvc.perform(get("/api/v1/customers/{id}/accounts/EUR", CUSTOMER_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currency").value("EUR"))
				.andExpect(content().string(containsString("\"balance\":100.50,")));
	}

	@Test
	void missingAccountIsNotFound() throws Exception
	{
		when(accountService.getAccount(CUSTOMER_ID, "EUR")).thenThrow(CustomerException.accountNotFound(CUSTOMER_ID, "EUR"));

		mockMvc.perform(get("/api/v1/customers/{id}/accounts/EUR", CUSTOMER_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.reason").value("ACCOUNT_NOT_FOUND"));
	}

	private ResultActions open(final String body) throws Exception
	{
		return mockMvc.perform(post("/api/v1/customers/{id}/accounts", CUSTOMER_ID).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private static AccountDto dto(final String currency, final String balance)
	{
		return new AccountDto(UUID.randomUUID(), CUSTOMER_ID, currency, new BigDecimal(balance), Instant.now());
	}

}
