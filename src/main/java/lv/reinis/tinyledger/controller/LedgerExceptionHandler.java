package lv.reinis.tinyledger.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lv.reinis.tinyledger.exception.CustomerException;
import lv.reinis.tinyledger.exception.TransactionException;


@RestControllerAdvice
public class LedgerExceptionHandler extends ResponseEntityExceptionHandler
{

	@ExceptionHandler(CustomerException.class)
	public ProblemDetail handleCustomerException(final CustomerException exception)
	{
		final HttpStatus status = switch (exception.getReason())
		{
			case CUSTOMER_NOT_FOUND, ACCOUNT_NOT_FOUND -> HttpStatus.NOT_FOUND;
			case ACCOUNT_ALREADY_EXISTS -> HttpStatus.CONFLICT;
			case INVALID_CURRENCY -> HttpStatus.BAD_REQUEST;
		};

		return problem(status, exception.getReason(), exception.getMessage());
	}

	@ExceptionHandler(TransactionException.class)
	public ProblemDetail handleTransactionException(final TransactionException exception)
	{
		final HttpStatus status = switch (exception.getReason())
		{
			case INVALID_AMOUNT -> HttpStatus.BAD_REQUEST;
			case INSUFFICIENT_FUNDS -> HttpStatus.UNPROCESSABLE_CONTENT;
		};

		return problem(status, exception.getReason(), exception.getMessage());
	}

	private static ProblemDetail problem(final HttpStatus status, final Enum<?> reason, final String message)
	{
		final ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
		problem.setProperty("reason", reason.name());

		return problem;
	}

}
