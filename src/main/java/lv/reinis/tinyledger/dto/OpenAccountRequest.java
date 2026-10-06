package lv.reinis.tinyledger.dto;

import jakarta.validation.constraints.NotBlank;


public record OpenAccountRequest(@NotBlank String currency)
{
}
