package com.safepay.schedule.controller;

import com.safepay.schedule.dto.ApiErrorResponse;
import com.safepay.schedule.dto.SchedulePaymentRequest;
import com.safepay.schedule.dto.SchedulePaymentResponse;
import com.safepay.schedule.service.PaymentScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment-schedules")
@Tag(name = "Payment Schedules", description = "Endpoints for managing payment scheduling operations")
public class PaymentScheduleController {

    private final PaymentScheduleService service;

    public PaymentScheduleController(PaymentScheduleService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(
            summary = "Create a payment schedule",
            description = "Schedules a future payment transfer and computes the applicable tax based on the scheduling horizon."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment schedule created successfully",
                    content = @Content(schema = @Schema(implementation = SchedulePaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Business rule violation",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<SchedulePaymentResponse> create(
            @Valid @RequestBody SchedulePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.schedule(request));
    }
}
