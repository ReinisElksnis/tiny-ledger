package lv.reinis.tinyledger.converter;

import java.util.List;

import org.springframework.stereotype.Component;

import lv.reinis.tinyledger.domain.Account;
import lv.reinis.tinyledger.domain.Customer;
import lv.reinis.tinyledger.dto.AccountDto;
import lv.reinis.tinyledger.dto.CustomerAccountsDto;


@Component
public class CustomerAccountsConverter
{

	private Converter<Account, AccountDto> accountConverter;

	public CustomerAccountsConverter(final Converter<Account, AccountDto> accountConverter)
	{
		this.accountConverter = accountConverter;
	}

	public CustomerAccountsDto convert(final Customer customer, final List<Account> accounts)
	{
		return new CustomerAccountsDto(customer.getId(), customer.getName(), customer.getCreatedAt(),
				accountConverter.convertAll(accounts));
	}

}
