CREATE TABLE customers
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(255)             NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE accounts
(
    id          UUID PRIMARY KEY,
    customer_id UUID                     NOT NULL,
    currency    VARCHAR(3)               NOT NULL,
    balance     DECIMAL(19, 3) DEFAULT 0 NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT uq_accounts_customer_currency UNIQUE (customer_id, currency),
    CONSTRAINT ck_accounts_balance_not_negative CHECK (balance >= 0)
);

CREATE TABLE transactions
(
    id            UUID PRIMARY KEY,
    account_id    UUID                     NOT NULL,
    type          VARCHAR(10)              NOT NULL,
    amount        DECIMAL(19, 3)           NOT NULL,
    balance_after DECIMAL(19, 3)           NOT NULL,
    description   VARCHAR(255),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT ck_transactions_type CHECK (type IN ('DEPOSIT', 'WITHDRAWAL')),
    CONSTRAINT ck_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_transactions_balance_after_not_negative CHECK (balance_after >= 0)
);

CREATE INDEX ix_transactions_account ON transactions (account_id, id);
