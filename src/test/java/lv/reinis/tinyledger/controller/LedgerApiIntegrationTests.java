package lv.reinis.tinyledger.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LedgerApiIntegrationTests
{

	@Autowired
	private MockMvc mockMvc;

	private String customerId;

	@BeforeEach
	void createCustomer() throws Exception
	{
		final String response = postJson("/api/v1/customers", "{\"name\": \"Anna\"}").andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		customerId = JsonPath.read(response, "$.id");
	}

	@Test
	void customerCanBeFetched() throws Exception
	{
		mockMvc.perform(get("/api/v1/customers/{id}", customerId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(customerId))
				.andExpect(jsonPath("$.name").value("Anna"))
				.andExpect(jsonPath("$.createdAt").isNotEmpty());
	}

	@Test
	void blankCustomerNameIsBadRequest() throws Exception
	{
		postJson("/api/v1/customers", "{\"name\": \" \"}").andExpect(status().isBadRequest());
	}

	@Test
	void unknownCustomerIsNotFound() throws Exception
	{
		mockMvc.perform(get("/api/v1/customers/{id}", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.reason").value("CUSTOMER_NOT_FOUND"));
	}

	@Test
	void malformedCustomerIdIsBadRequest() throws Exception
	{
		mockMvc.perform(get("/api/v1/customers/not-a-uuid")).andExpect(status().isBadRequest());
	}

	@Test
	void accountsCanBeOpenedListedAndFetched() throws Exception
	{
		openAccount("jpy").andExpect(status().isCreated())
				.andExpect(jsonPath("$.currency").value("JPY"))
				.andExpect(jsonPath("$.customerId").value(customerId))
				.andExpect(jsonPath("$.balance").value(0));
		openAccount("EUR").andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/customers/{id}/accounts", customerId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].currency", contains("EUR", "JPY")));
		mockMvc.perform(get("/api/v1/customers/{id}/accounts/eur", customerId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currency").value("EUR"));
	}

	@Test
	void duplicateAccountIsConflict() throws Exception
	{
		openAccount("EUR").andExpect(status().isCreated());

		openAccount("EUR").andExpect(status().isConflict()).andExpect(jsonPath("$.reason").value("ACCOUNT_ALREADY_EXISTS"));
	}

	@Test
	void invalidCurrencyIsBadRequest() throws Exception
	{
		openAccount("ZZZ").andExpect(status().isBadRequest()).andExpect(jsonPath("$.reason").value("INVALID_CURRENCY"));
	}

	@Test
	void missingAccountIsNotFound() throws Exception
	{
		mockMvc.perform(get("/api/v1/customers/{id}/accounts/EUR", customerId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.reason").value("ACCOUNT_NOT_FOUND"));
	}

	@Test
	void depositsAndWithdrawalsMoveBalanceAndAppearInHistory() throws Exception
	{
		openAccount("EUR");

		movement("{\"type\": \"DEPOSIT\", \"amount\": 100.50, \"description\": \"salary\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("DEPOSIT"))
				.andExpect(jsonPath("$.amount").value(100.50))
				.andExpect(jsonPath("$.balanceAfter").value(100.50));
		movement("{\"type\": \"WITHDRAWAL\", \"amount\": 30.25, \"description\": \"rent\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("WITHDRAWAL"))
				.andExpect(jsonPath("$.balanceAfter").value(70.25));

		mockMvc.perform(get("/api/v1/customers/{id}/accounts/EUR", customerId)).andExpect(jsonPath("$.balance").value(70.25));
		mockMvc.perform(get("/api/v1/customers/{id}/accounts/EUR/transactions", customerId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[*].description", contains("rent", "salary")));
	}

	@Test
	void overdraftIsUnprocessable() throws Exception
	{
		openAccount("EUR");
		movement("{\"type\": \"DEPOSIT\", \"amount\": 5}");

		movement("{\"type\": \"WITHDRAWAL\", \"amount\": 5.01}")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.reason").value("INSUFFICIENT_FUNDS"));
	}

	@Test
	void invalidAmountsAreBadRequest() throws Exception
	{
		openAccount("EUR");

		movement("{\"type\": \"DEPOSIT\", \"amount\": 0}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.reason").value("INVALID_AMOUNT"));
		movement("{\"type\": \"DEPOSIT\", \"amount\": 1.234}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.reason").value("INVALID_AMOUNT"));
		movement("{\"type\": \"DEPOSIT\"}").andExpect(status().isBadRequest());
		movement("{\"type\": \"DEPOSIT\", \"amount\": \"abc\"}").andExpect(status().isBadRequest());
	}

	@Test
	void missingOrUnknownTransactionTypeIsBadRequest() throws Exception
	{
		openAccount("EUR");

		movement("{\"amount\": 5}").andExpect(status().isBadRequest());
		movement("{\"type\": null, \"amount\": 5}").andExpect(status().isBadRequest());
		movement("{\"type\": \"TRANSFER\", \"amount\": 5}").andExpect(status().isBadRequest());
		movement("{\"type\": \"\", \"amount\": 5}").andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/v1/customers/{id}/accounts/EUR/transactions", customerId)).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void amountsAreRenderedWithCurrencyDecimalPlaces() throws Exception
	{
		openAccount("EUR").andExpect(content().string(containsString("\"balance\":0.00,")));
		openAccount("JPY").andExpect(content().string(containsString("\"balance\":0,")));
		openAccount("KWD").andExpect(content().string(containsString("\"balance\":0.000,")));

		movement("EUR", "{\"type\": \"DEPOSIT\", \"amount\": 100.5}")
				.andExpect(content().string(containsString("\"amount\":100.50,")))
				.andExpect(content().string(containsString("\"balanceAfter\":100.50,")));
		movement("JPY", "{\"type\": \"DEPOSIT\", \"amount\": 1000.00}")
				.andExpect(content().string(containsString("\"amount\":1000,")))
				.andExpect(content().string(containsString("\"balanceAfter\":1000,")));
		movement("KWD", "{\"type\": \"DEPOSIT\", \"amount\": 1.2}").andExpect(content().string(containsString("\"amount\":1.200,")));

		mockMvc.perform(get("/api/v1/customers/{id}/accounts/EUR", customerId))
				.andExpect(content().string(containsString("\"balance\":100.50,")));
		mockMvc.perform(get("/api/v1/customers/{id}/accounts/JPY/transactions", customerId))
				.andExpect(content().string(containsString("\"amount\":1000,")));
	}

	@Test
	void fractionalAmountInNonFractionalCurrencyIsBadRequest() throws Exception
	{
		openAccount("JPY");

		movement("JPY", "{\"type\": \"DEPOSIT\", \"amount\": 10.5}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.reason").value("INVALID_AMOUNT"));
		movement("JPY", "{\"type\": \"DEPOSIT\", \"amount\": 100}").andExpect(status().isCreated());
		movement("JPY", "{\"type\": \"WITHDRAWAL\", \"amount\": 0.5}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.reason").value("INVALID_AMOUNT"));
	}

	@Test
	void movementOnMissingAccountIsNotFound() throws Exception
	{
		movement("{\"type\": \"DEPOSIT\", \"amount\": 1}").andExpect(status().isNotFound()).andExpect(jsonPath("$.reason").value("ACCOUNT_NOT_FOUND"));
	}

	private ResultActions openAccount(final String currency) throws Exception
	{
		return postJson("/api/v1/customers/" + customerId + "/accounts", "{\"currency\": \"" + currency + "\"}");
	}

	private ResultActions movement(final String body) throws Exception
	{
		return movement("EUR", body);
	}

	private ResultActions movement(final String currency, final String body) throws Exception
	{
		return postJson("/api/v1/customers/" + customerId + "/accounts/" + currency + "/transactions", body);
	}

	private ResultActions postJson(final String path, final String body) throws Exception
	{
		return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
	}

}
