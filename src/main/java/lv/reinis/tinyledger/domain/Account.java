package lv.reinis.tinyledger.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "accounts", uniqueConstraints = @UniqueConstraint(name = "uq_accounts_customer_currency", columnNames = {
		"customer_id", "currency" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account
{

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@Column(nullable = false, length = 3)
	private String currency;

	@Setter
	@Column(nullable = false, precision = 19, scale = 3)
	private BigDecimal balance = BigDecimal.ZERO;

	@CreationTimestamp
	@Column(nullable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant modifiedAt;

	public Account(final Customer customer, final String currency)
	{
		this.customer = customer;
		this.currency = currency;
	}

}
