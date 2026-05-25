CREATE TABLE payment_schedule (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    origin_account  VARCHAR(30)     NOT NULL,
    destination_account VARCHAR(30) NOT NULL,
    amount          NUMERIC(15, 2)  NOT NULL,
    tax             NUMERIC(15, 2)  NOT NULL,
    scheduled_date  DATE            NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),

    CONSTRAINT pk_payment_schedule PRIMARY KEY (id),
    CONSTRAINT chk_amount_positive  CHECK (amount > 0),
    CONSTRAINT chk_tax_non_negative CHECK (tax >= 0)
);

COMMENT ON TABLE  payment_schedule                   IS 'Stores all scheduled payment transfers';
COMMENT ON COLUMN payment_schedule.amount            IS 'Gross transfer amount in BRL';
COMMENT ON COLUMN payment_schedule.tax               IS 'Computed fee applied at scheduling time';
COMMENT ON COLUMN payment_schedule.scheduled_date    IS 'Future date on which the transfer will be executed';
