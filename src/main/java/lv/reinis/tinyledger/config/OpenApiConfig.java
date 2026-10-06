package lv.reinis.tinyledger.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;


@Configuration
public class OpenApiConfig
{

	@Bean
	public OpenAPI tinyLedgerOpenApi()
	{
		return new OpenAPI().info(new Info().title("Tiny Ledger API").version("v1")
				.description("Customers hold one account per currency; deposits and withdrawals move the balance and are kept as history."));
	}

}
