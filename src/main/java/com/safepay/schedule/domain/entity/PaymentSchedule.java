package com.safepay.schedule.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_schedule")
public class PaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "origin_account", nullable = false, length = 30)
    private String originAccount;

    @Column(name = "destination_account", nullable = false, length = 30)
    private String destinationAccount;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "tax", nullable = false, precision = 15, scale = 2)
    private BigDecimal tax;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PaymentSchedule() {}

    private PaymentSchedule(Builder builder) {
        this.originAccount      = builder.originAccount;
        this.destinationAccount = builder.destinationAccount;
        this.amount             = builder.amount;
        this.tax                = builder.tax;
        this.scheduledDate      = builder.scheduledDate;
        this.createdAt          = LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId()                  { return id; }
    public String getOriginAccount()     { return originAccount; }
    public String getDestinationAccount(){ return destinationAccount; }
    public BigDecimal getAmount()        { return amount; }
    public BigDecimal getTax()           { return tax; }
    public LocalDate getScheduledDate()  { return scheduledDate; }
    public LocalDateTime getCreatedAt()  { return createdAt; }

    public static final class Builder {
        private String originAccount;
        private String destinationAccount;
        private BigDecimal amount;
        private BigDecimal tax;
        private LocalDate scheduledDate;

        private Builder() {}

        public Builder originAccount(String val)      { originAccount = val;      return this; }
        public Builder destinationAccount(String val) { destinationAccount = val; return this; }
        public Builder amount(BigDecimal val)         { amount = val;             return this; }
        public Builder tax(BigDecimal val)            { tax = val;                return this; }
        public Builder scheduledDate(LocalDate val)   { scheduledDate = val;      return this; }

        public PaymentSchedule build() { return new PaymentSchedule(this); }
    }
}
