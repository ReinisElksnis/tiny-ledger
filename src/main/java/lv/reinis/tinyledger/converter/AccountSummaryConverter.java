package lv.reinis.tinyledger.converter;

import org.springframework.stereotype.Component;

import lv.reinis.tinyledger.domain.Account;
import lv.reinis.tinyledger.dto.AccountSummaryDto;
import lv.reinis.tinyledger.util.CurrencyUtils;


@Component
public class AccountSummaryConverter implements Converter<Account, AccountSummaryDto>
{

	@Override
	public AccountSummaryDto convert(final Account source)
	{
		final String currency = source.getCurrency();

		return new AccountSummaryDto(source.getId(), currency, CurrencyUtils.scale(source.getBalance(), currency),
				source.getCreatedAt());
	}

}
