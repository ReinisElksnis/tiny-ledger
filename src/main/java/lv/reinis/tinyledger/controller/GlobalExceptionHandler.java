package lv.reinis.tinyledger.controller;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lv.reinis.tinyledger.exception.CustomerException;
import lv.reinis.tinyledger.exception.TransactionException;
import tools.jackson.databind.exc.MismatchedInputException;


@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler
{

	private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private static final String VALIDATION_FAILED = "Validation failed";

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

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpectedException(final Exception exception)
	{
		LOG.error("Unhandled exception", exception);

		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(final MethodArgumentNotValidException exception,
			final HttpHeaders headers, final HttpStatusCode status, final WebRequest request)
	{
		final Map<String, String> errors = new TreeMap<>();

		for (final FieldError fieldError : exception.getBindingResult().getFieldErrors())
		{
			errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
		}

		return handleExceptionInternal(exception, validationProblem(status, errors), headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(final HttpMessageNotReadableException exception,
			final HttpHeaders headers, final HttpStatusCode status, final WebRequest request)
	{
		// A value of the wrong type (unknown transaction type, text for an amount) is reported against its field.
		if (exception.getCause() instanceof final MismatchedInputException mismatch && !mismatch.getPath().isEmpty())
		{
			final String field = mismatch.getPath().getLast().getPropertyName();

			if (field != null)
			{
				final Map<String, String> errors = Map.of(field, invalidValueMessage(mismatch.getTargetType()));

				return handleExceptionInternal(exception, validationProblem(status, errors), headers, status, request);
			}
		}

		return super.handleHttpMessageNotReadable(exception, headers, status, request);
	}

	private static String invalidValueMessage(final Class<?> targetType)
	{
		if (targetType != null && targetType.isEnum())
		{
			return "must be one of " + Arrays.stream(targetType.getEnumConstants()).map(Object::toString).collect(Collectors.joining(", "));
		}

		return "has an invalid value";
	}

	private static ProblemDetail validationProblem(final HttpStatusCode status, final Map<String, String> errors)
	{
		final ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, VALIDATION_FAILED);
		problem.setProperty("errors", errors);

		return problem;
	}

	private static ProblemDetail problem(final HttpStatus status, final Enum<?> reason, final String message)
	{
		final ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
		problem.setProperty("reason", reason.name());

		return problem;
	}

}
