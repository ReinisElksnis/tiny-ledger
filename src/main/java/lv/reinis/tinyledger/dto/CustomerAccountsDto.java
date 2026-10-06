package lv.reinis.tinyledger.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


public record CustomerAccountsDto(UUID id, String name, Instant createdAt, List<AccountDto> accounts)
{
}
