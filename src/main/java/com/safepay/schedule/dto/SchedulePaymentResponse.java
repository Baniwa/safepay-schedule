package com.safepay.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.safepay.schedule.domain.entity.PaymentSchedule;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Confirmed payment schedule with computed tax")
public record SchedulePaymentResponse(

        @Schema(description = "Unique schedule identifier")
        UUID id,

        String originAccount,
        String destinationAccount,

        @Schema(description = "Gross transfer amount in BRL")
        BigDecimal amount,

        @Schema(description = "Computed fee applied at scheduling time")
        BigDecimal tax,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate scheduledDate,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static SchedulePaymentResponse from(PaymentSchedule entity) {
        return new SchedulePaymentResponse(
                entity.getId(),
                entity.getOriginAccount(),
                entity.getDestinationAccount(),
                entity.getAmount(),
                entity.getTax(),
                entity.getScheduledDate(),
                entity.getCreatedAt()
        );
    }
}
