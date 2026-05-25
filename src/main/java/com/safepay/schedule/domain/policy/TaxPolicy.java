package com.safepay.schedule.domain.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class TaxPolicy {

    private static final long LONG_HORIZON_THRESHOLD_DAYS = 10L;
    private static final BigDecimal LONG_HORIZON_RATE  = new BigDecimal("0.02");
    private static final BigDecimal SHORT_HORIZON_RATE = new BigDecimal("0.05");

    private TaxPolicy() {}

    public static BigDecimal compute(BigDecimal amount, LocalDate scheduledDate) {
        long daysUntilExecution = ChronoUnit.DAYS.between(LocalDate.now(), scheduledDate);
        BigDecimal rate = daysUntilExecution > LONG_HORIZON_THRESHOLD_DAYS
                ? LONG_HORIZON_RATE
                : SHORT_HORIZON_RATE;
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
