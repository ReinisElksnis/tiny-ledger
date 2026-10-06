package lv.reinis.tinyledger.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lv.reinis.tinyledger.domain.TransactionType;


public record TransactionDto(UUID id, UUID accountId, TransactionType type, BigDecimal amount, BigDecimal balanceAfter,
		String description, Instant createdAt)
{
}
