package com.safepay.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Payload for creating a new payment schedule")
public record SchedulePaymentRequest(

        @Schema(description = "Sender's account number", example = "123456789")
        @NotBlank(message = "Origin account is required")
        @Size(max = 30, message = "Origin account must not exceed 30 characters")
        String originAccount,

        @Schema(description = "Recipient's account number", example = "987654321")
        @NotBlank(message = "Destination account is required")
        @Size(max = 30, message = "Destination account must not exceed 30 characters")
        String destinationAccount,

        @Schema(description = "Gross transfer amount in BRL", example = "1500.00")
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @Schema(description = "Future date for executing the transfer", example = "2026-06-30")
        @NotNull(message = "Scheduled date is required")
        @Future(message = "Scheduled date must be a future date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate scheduledDate
) {}
