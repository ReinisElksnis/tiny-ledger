package lv.reinis.tinyledger.converter;

import org.springframework.stereotype.Component;

import lv.reinis.tinyledger.domain.Transaction;
import lv.reinis.tinyledger.dto.TransactionDto;
import lv.reinis.tinyledger.util.CurrencyUtils;


@Component
public class TransactionConverter implements Converter<Transaction, TransactionDto>
{

	@Override
	public TransactionDto convert(final Transaction source)
	{
		final String currency = source.getAccount().getCurrency();

		return new TransactionDto(source.getId(), source.getAccount().getId(), source.getType(),
				CurrencyUtils.scale(source.getAmount(), currency), CurrencyUtils.scale(source.getBalanceAfter(), currency),
				source.getDescription(), source.getCreatedAt());
	}

}
