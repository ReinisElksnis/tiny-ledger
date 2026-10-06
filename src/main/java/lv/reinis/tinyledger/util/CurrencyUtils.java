package lv.reinis.tinyledger.util;

import java.math.BigDecimal;
import java.util.Currency;


public final class CurrencyUtils
{
	public static int fractionDigits(final Currency currency)
	{
		return Math.max(0, currency.getDefaultFractionDigits());
	}

	public static BigDecimal scale(final BigDecimal amount, final String currencyCode)
	{
		return amount.setScale(fractionDigits(Currency.getInstance(currencyCode)));
	}

}
