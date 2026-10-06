package lv.reinis.tinyledger.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import lv.reinis.tinyledger.dto.CustomerAccountsDto;
import lv.reinis.tinyledger.dto.CustomerDto;
import lv.reinis.tinyledger.exception.CustomerException;
import lv.reinis.tinyledger.service.CustomerService;


@ExtendWith(MockitoExtension.class)
class CustomerControllerTests
{

	private static final UUID CUSTOMER_ID = UUID.randomUUID();

	@Mock
	private CustomerService customerService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp()
	{
		mockMvc = MockMvcBuilders.standaloneSetup(new CustomerController(customerService))
				.setControllerAdvice(new GlobalExceptionHandler()).build();
	}

	@Test
	void createReturnsCreatedCustomer() throws Exception
	{
		when(customerService.create("Anna")).thenReturn(new CustomerDto(CUSTOMER_ID, "Anna", Instant.now()));

		create("{\"name\": \"Anna\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(CUSTOMER_ID.toString()))
				.andExpect(jsonPath("$.name").value("Anna"))
				.andExpect(jsonPath("$.createdAt").isNotEmpty());
	}

	@Test
	void invalidNameIsBadRequest() throws Exception
	{
		create("{}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Validation failed"))
				.andExpect(jsonPath("$.errors.name").isNotEmpty());
		create("{\"name\": \" \"}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.name").isNotEmpty());
		create("{\"name\": \"" + "a".repeat(256) + "\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").isNotEmpty());

		verifyNoInteractions(customerService);
	}

	@Test
	void getReturnsCustomer() throws Exception
	{
		when(customerService.get(CUSTOMER_ID)).thenReturn(new CustomerDto(CUSTOMER_ID, "Anna", Instant.now()));

		mockMvc.perform(get("/api/v1/customers/{id}", CUSTOMER_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(CUSTOMER_ID.toString()))
				.andExpect(jsonPath("$.name").value("Anna"));
	}

	@Test
	void listReturnsCustomersWithAccounts() throws Exception
	{
		final UUID otherId = UUID.randomUUID();
		final AccountDto euro = new AccountDto(UUID.randomUUID(), CUSTOMER_ID, "EUR", new BigDecimal("100.50"), Instant.now());
		final AccountDto yen = new AccountDto(UUID.randomUUID(), CUSTOMER_ID, "JPY", new BigDecimal("1000"), Instant.now());
		when(customerService.listWithAccounts()).thenReturn(List.of(
				new CustomerAccountsDto(CUSTOMER_ID, "Anna", Instant.now(), List.of(euro, yen)),
				new CustomerAccountsDto(otherId, "Marta", Instant.now(), List.of())));

		mockMvc.perform(get("/api/v1/customers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", contains("Anna", "Marta")))
				.andExpect(jsonPath("$[0].id").value(CUSTOMER_ID.toString()))
				.andExpect(jsonPath("$[0].accounts[*].currency", contains("EUR", "JPY")))
				.andExpect(jsonPath("$[1].accounts", hasSize(0)));
	}

	@Test
	void unknownCustomerIsNotFound() throws Exception
	{
		when(customerService.get(CUSTOMER_ID)).thenThrow(CustomerException.customerNotFound(CUSTOMER_ID));

		mockMvc.perform(get("/api/v1/customers/{id}", CUSTOMER_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.reason").value("CUSTOMER_NOT_FOUND"));
	}

	@Test
	void unexpectedErrorIsInternalServerErrorWithoutDetails() throws Exception
	{
		when(customerService.get(CUSTOMER_ID)).thenThrow(new IllegalStateException("database is on fire"));

		mockMvc.perform(get("/api/v1/customers/{id}", CUSTOMER_ID))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.detail").value("Internal server error"));
	}

	@Test
	void malformedJsonIsBadRequest() throws Exception
	{
		create("{not json").andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").doesNotExist());

		verifyNoInteractions(customerService);
	}

	@Test
	void malformedCustomerIdIsBadRequest() throws Exception
	{
		mockMvc.perform(get("/api/v1/customers/not-a-uuid")).andExpect(status().isBadRequest());

		verifyNoInteractions(customerService);
	}

	private ResultActions create(final String body) throws Exception
	{
		return mockMvc.perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON).content(body));
	}

}
