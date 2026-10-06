package lv.reinis.tinyledger.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import lv.reinis.tinyledger.exception.CustomerException;


class CurrencyUtilsTests
{

	@Test
	void currencyCodeIsParsedIgnoringCase()
	{
		assertThat(CurrencyUtils.parseCurrency("eur")).isEqualTo(Currency.getInstance("EUR"));
		assertThat(CurrencyUtils.parseCurrency("JPY")).isEqualTo(Currency.getInstance("JPY"));
	}

	@Test
	void unknownOrMissingCurrencyCodeIsRejected()
	{
		assertThatThrownBy(() -> CurrencyUtils.parseCurrency("ZZZ")).isInstanceOf(CustomerException.class)
				.hasFieldOrPropertyWithValue("reason", CustomerException.Reason.INVALID_CURRENCY);
		assertThatThrownBy(() -> CurrencyUtils.parseCurrency(null)).isInstanceOf(CustomerException.class)
				.hasFieldOrPropertyWithValue("reason", CustomerException.Reason.INVALID_CURRENCY);
	}

	@Test
	void fractionDigitsFollowCurrency()
	{
		assertThat(CurrencyUtils.fractionDigits(Currency.getInstance("EUR"))).isEqualTo(2);
		assertThat(CurrencyUtils.fractionDigits(Currency.getInstance("JPY"))).isZero();
		assertThat(CurrencyUtils.fractionDigits(Currency.getInstance("KWD"))).isEqualTo(3);
	}

	@Test
	void currencyWithoutDefinedFractionDigitsHasNone()
	{
		assertThat(CurrencyUtils.fractionDigits(Currency.getInstance("XAU"))).isZero();
	}

	@Test
	void scaleIsSetToCurrencyFractionDigits()
	{
		// isEqualTo, unlike isEqualByComparingTo, also checks the scale.
		assertThat(CurrencyUtils.scale(new BigDecimal("100.500"), "EUR")).isEqualTo(new BigDecimal("100.50"));
		assertThat(CurrencyUtils.scale(new BigDecimal("1000.000"), "JPY")).isEqualTo(new BigDecimal("1000"));
		assertThat(CurrencyUtils.scale(new BigDecimal("1.2"), "KWD")).isEqualTo(new BigDecimal("1.200"));
	}

	@Test
	void scaleNeverRounds()
	{
		assertThatThrownBy(() -> CurrencyUtils.scale(new BigDecimal("10.555"), "EUR")).isInstanceOf(ArithmeticException.class);
	}

}
