package lv.reinis.tinyledger.dto;

import java.time.Instant;
import java.util.UUID;


public record CustomerDto(UUID id, String name, Instant createdAt)
{
}
