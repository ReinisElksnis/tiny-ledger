package lv.reinis.tinyledger.util;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;

import lv.reinis.tinyledger.exception.CustomerException;


public final class CurrencyUtils
{
	public static Currency parseCurrency(final String currency)
	{
		try
		{
			return Currency.getInstance(currency.toUpperCase(Locale.ROOT));
		}
		catch (final IllegalArgumentException | NullPointerException e)
		{
			throw CustomerException.invalidCurrency(currency);
		}
	}

	public static int fractionDigits(final Currency currency)
	{
		return Math.max(0, currency.getDefaultFractionDigits());
	}

	public static BigDecimal scale(final BigDecimal amount, final String currencyCode)
	{
		return amount.setScale(fractionDigits(Currency.getInstance(currencyCode)));
	}

}
