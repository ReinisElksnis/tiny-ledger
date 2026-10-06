package lv.reinis.tinyledger.exception;

import java.math.BigDecimal;


public class TransactionException extends RuntimeException
{

	public enum Reason
	{
		INVALID_AMOUNT,
		INSUFFICIENT_FUNDS
	}

	private final Reason reason;

	private TransactionException(final Reason reason, final String message)
	{
		super(message);
		this.reason = reason;
	}

	public Reason getReason()
	{
		return reason;
	}

	public static TransactionException invalidAmount(final String message)
	{
		return new TransactionException(Reason.INVALID_AMOUNT, message);
	}

	public static TransactionException insufficientFunds(final String currency, final BigDecimal balance, final BigDecimal amount)
	{
		return new TransactionException(Reason.INSUFFICIENT_FUNDS,
				"Cannot withdraw %s %s: balance is %s %s".formatted(amount.toPlainString(), currency, balance.toPlainString(), currency));
	}

}
