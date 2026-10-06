package lv.reinis.tinyledger.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
				.setControllerAdvice(new LedgerExceptionHandler()).build();
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
		create("{}").andExpect(status().isBadRequest());
		create("{\"name\": \" \"}").andExpect(status().isBadRequest());
		create("{\"name\": \"" + "a".repeat(256) + "\"}").andExpect(status().isBadRequest());

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
	void unknownCustomerIsNotFound() throws Exception
	{
		when(customerService.get(CUSTOMER_ID)).thenThrow(CustomerException.customerNotFound(CUSTOMER_ID));

		mockMvc.perform(get("/api/v1/customers/{id}", CUSTOMER_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.reason").value("CUSTOMER_NOT_FOUND"));
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
