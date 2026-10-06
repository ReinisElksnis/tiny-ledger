package lv.reinis.tinyledger;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import lv.reinis.tinyledger.dto.AccountDto;
import lv.reinis.tinyledger.dto.TransactionDto;
import lv.reinis.tinyledger.service.AccountService;
import lv.reinis.tinyledger.service.CustomerService;
import lv.reinis.tinyledger.service.TransactionService;


@SpringBootTest
@ActiveProfiles("demo")
class DemoDataIntegrationTests
{

	private static final UUID ANNA = UUID.fromString("00000000-0000-7000-8000-000000000001");

	private static final UUID MARTA = UUID.fromString("00000000-0000-7000-8000-000000000002");

	@Autowired
	private CustomerService customerService;

	@Autowired
	private AccountService accountService;

	@Autowired
	private TransactionService transactionService;

	@Test
	void demoCustomersAndAccountsAreLoaded()
	{
		assertThat(customerService.get(ANNA).name()).isEqualTo("Anna Ozola");
		assertThat(customerService.get(MARTA).name()).isEqualTo("Marta Liepa");
		assertThat(accountService.listAccounts(ANNA)).extracting(AccountDto::currency).containsExactly("EUR", "JPY");
		assertThat(accountService.listAccounts(MARTA)).extracting(AccountDto::currency).containsExactly("EUR", "KWD");
	}

	@Test
	void demoBalancesMatchLatestTransaction()
	{
		assertBalanceMatchesHistory(ANNA, "EUR", "1250.50", 3);
		assertBalanceMatchesHistory(ANNA, "JPY", "50000", 1);
		assertBalanceMatchesHistory(MARTA, "KWD", "12.345", 2);
		assertThat(accountService.getAccount(MARTA, "EUR").balance()).isEqualTo(new BigDecimal("0.00"));
		assertThat(transactionService.history(MARTA, "EUR")).isEmpty();
	}

	private void assertBalanceMatchesHistory(final UUID customerId, final String currency, final String balance, final int transactions)
	{
		final List<TransactionDto> history = transactionService.history(customerId, currency);

		assertThat(history).hasSize(transactions);
		assertThat(history.getFirst().balanceAfter()).isEqualTo(new BigDecimal(balance));
		assertThat(accountService.getAccount(customerId, currency).balance()).isEqualTo(new BigDecimal(balance));
	}

}
