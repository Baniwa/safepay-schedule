package com.safepay.schedule.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safepay.schedule.domain.exception.BusinessException;
import com.safepay.schedule.dto.SchedulePaymentRequest;
import com.safepay.schedule.dto.SchedulePaymentResponse;
import com.safepay.schedule.service.PaymentScheduleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentScheduleController.class)
@DisplayName("PaymentScheduleController")
class PaymentScheduleControllerTest {

    private static final String ENDPOINT = "/api/v1/payment-schedules";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentScheduleService service;

    @Test
    @DisplayName("POST returns 201 with computed tax on valid request")
    void returnsCreatedOnSuccess() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(15);
        SchedulePaymentRequest request = new SchedulePaymentRequest(
                "ACC-001", "ACC-002", new BigDecimal("1000.00"), futureDate);

        SchedulePaymentResponse response = new SchedulePaymentResponse(
                UUID.randomUUID(), "ACC-001", "ACC-002",
                new BigDecimal("1000.00"), new BigDecimal("20.00"),
                futureDate, LocalDateTime.now());

        when(service.schedule(any())).thenReturn(response);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tax").value("20.0"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @DisplayName("POST returns 400 when required fields are missing")
    void returns400OnMissingFields() throws Exception {
        String emptyPayload = "{}";

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations").isArray());
    }

    @Test
    @DisplayName("POST returns 422 on business rule violation")
    void returns422OnBusinessRuleViolation() throws Exception {
        SchedulePaymentRequest request = new SchedulePaymentRequest(
                "ACC-001", "ACC-002", new BigDecimal("500.00"), LocalDate.now().plusDays(5));

        when(service.schedule(any()))
                .thenThrow(new BusinessException("Scheduled date must be strictly in the future.", HttpStatus.UNPROCESSABLE_ENTITY));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Scheduled date must be strictly in the future."));
    }
}
