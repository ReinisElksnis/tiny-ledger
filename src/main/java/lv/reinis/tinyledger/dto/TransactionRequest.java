package lv.reinis.tinyledger.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lv.reinis.tinyledger.domain.TransactionType;


public record TransactionRequest(@NotNull TransactionType type, @NotNull BigDecimal amount, @Size(max = 255) String description)
{
}
