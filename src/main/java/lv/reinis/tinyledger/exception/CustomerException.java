package lv.reinis.tinyledger.exception;

import java.util.UUID;


public class CustomerException extends RuntimeException
{

	public enum Reason
	{
		CUSTOMER_NOT_FOUND,
		ACCOUNT_NOT_FOUND,
		ACCOUNT_ALREADY_EXISTS,
		INVALID_CURRENCY
	}

	private final Reason reason;

	private CustomerException(final Reason reason, final String message)
	{
		super(message);
		this.reason = reason;
	}

	public Reason getReason()
	{
		return reason;
	}

	public static CustomerException customerNotFound(final UUID customerId)
	{
		return new CustomerException(Reason.CUSTOMER_NOT_FOUND, "Customer %s not found".formatted(customerId));
	}

	public static CustomerException accountNotFound(final UUID customerId, final String currency)
	{
		return new CustomerException(Reason.ACCOUNT_NOT_FOUND, "Customer %s has no %s account".formatted(customerId, currency));
	}

	public static CustomerException accountAlreadyExists(final UUID customerId, final String currency)
	{
		return new CustomerException(Reason.ACCOUNT_ALREADY_EXISTS, "Customer %s already has a %s account".formatted(customerId, currency));
	}

	public static CustomerException invalidCurrency(final String currency)
	{
		return new CustomerException(Reason.INVALID_CURRENCY, "'%s' is not an ISO 4217 currency code".formatted(currency));
	}

}
