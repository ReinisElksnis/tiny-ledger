package lv.reinis.tinyledger.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


public record AccountDto(UUID id, UUID customerId, String currency, BigDecimal balance, Instant createdAt)
{
}
