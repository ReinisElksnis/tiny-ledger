package lv.reinis.tinyledger.converter;

import org.springframework.stereotype.Component;

import lv.reinis.tinyledger.domain.Account;
import lv.reinis.tinyledger.dto.AccountDto;


@Component
public class AccountConverter implements Converter<Account, AccountDto>
{

	@Override
	public AccountDto convert(final Account source)
	{
		return new AccountDto(source.getId(), source.getCustomer().getId(), source.getCurrency(), source.getBalance(),
				source.getCreatedAt());
	}

}
