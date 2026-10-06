package lv.reinis.tinyledger.converter;

import java.util.Collection;
import java.util.List;


public interface Converter<SOURCE, TARGET>
{

	TARGET convert(final SOURCE source);

	default List<TARGET> convertAll(final Collection<? extends SOURCE> sources)
	{
		return sources.stream().map(this::convert).toList();
	}

}
