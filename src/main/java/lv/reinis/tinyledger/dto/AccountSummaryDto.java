package lv.reinis.tinyledger.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


public record AccountSummaryDto(UUID id, String currency, BigDecimal balance, Instant createdAt)
{
}
