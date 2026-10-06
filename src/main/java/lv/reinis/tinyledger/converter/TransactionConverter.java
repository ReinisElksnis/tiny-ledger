package lv.reinis.tinyledger.converter;

import org.springframework.stereotype.Component;

import lv.reinis.tinyledger.domain.Transaction;
import lv.reinis.tinyledger.dto.TransactionDto;


@Component
public class TransactionConverter implements Converter<Transaction, TransactionDto>
{

	@Override
	public TransactionDto convert(final Transaction source)
	{
		return new TransactionDto(source.getId(), source.getAccount().getId(), source.getType(), source.getAmount(),
				source.getBalanceAfter(), source.getDescription(), source.getCreatedAt());
	}

}
