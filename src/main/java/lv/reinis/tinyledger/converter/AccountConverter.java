package lv.reinis.tinyledger.converter;

import org.springframework.stereotype.Component;

import lv.reinis.tinyledger.domain.Account;
import lv.reinis.tinyledger.dto.AccountDto;
import lv.reinis.tinyledger.util.CurrencyUtils;


@Component
public class AccountConverter implements Converter<Account, AccountDto>
{

	@Override
	public AccountDto convert(final Account source)
	{
		final String currency = source.getCurrency();

		return new AccountDto(source.getId(), source.getCustomer().getId(), currency,
				CurrencyUtils.scale(source.getBalance(), currency), source.getCreatedAt());
	}

}
