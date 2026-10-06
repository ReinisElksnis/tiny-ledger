package lv.reinis.tinyledger.converter;

import org.springframework.stereotype.Component;

import lv.reinis.tinyledger.domain.Customer;
import lv.reinis.tinyledger.dto.CustomerDto;


@Component
public class CustomerConverter implements Converter<Customer, CustomerDto>
{

	@Override
	public CustomerDto convert(final Customer source)
	{
		return new CustomerDto(source.getId(), source.getName(), source.getCreatedAt());
	}

}
