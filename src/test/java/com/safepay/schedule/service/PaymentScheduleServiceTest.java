package com.safepay.schedule.service;

import com.safepay.schedule.domain.entity.PaymentSchedule;
import com.safepay.schedule.domain.exception.BusinessException;
import com.safepay.schedule.dto.SchedulePaymentRequest;
import com.safepay.schedule.dto.SchedulePaymentResponse;
import com.safepay.schedule.repository.PaymentScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentScheduleService")
class PaymentScheduleServiceTest {

    @Mock
    private PaymentScheduleRepository repository;

    @InjectMocks
    private PaymentScheduleService service;

    @Nested
    @DisplayName("when scheduling a valid payment")
    class ValidPayment {

        @Test
        @DisplayName("applies 5% tax when scheduled within 10 days")
        void appliesShortHorizonTax() {
            LocalDate nearDate = LocalDate.now().plusDays(3);
            SchedulePaymentRequest request = buildRequest(new BigDecimal("1000.00"), nearDate);

            PaymentSchedule stubEntity = PaymentSchedule.builder()
                    .originAccount("ACC-001")
                    .destinationAccount("ACC-002")
                    .amount(new BigDecimal("1000.00"))
                    .tax(new BigDecimal("50.00"))
                    .scheduledDate(nearDate)
                    .build();

            when(repository.save(any(PaymentSchedule.class))).thenReturn(stubEntity);

            SchedulePaymentResponse response = service.schedule(request);

            assertThat(response.tax()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("applies 2% tax when scheduled beyond 10 days")
        void appliesLongHorizonTax() {
            LocalDate farDate = LocalDate.now().plusDays(15);
            SchedulePaymentRequest request = buildRequest(new BigDecimal("1000.00"), farDate);

            PaymentSchedule stubEntity = PaymentSchedule.builder()
                    .originAccount("ACC-001")
                    .destinationAccount("ACC-002")
                    .amount(new BigDecimal("1000.00"))
                    .tax(new BigDecimal("20.00"))
                    .scheduledDate(farDate)
                    .build();

            when(repository.save(any(PaymentSchedule.class))).thenReturn(stubEntity);

            SchedulePaymentResponse response = service.schedule(request);

            assertThat(response.tax()).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("persists entity with computed tax")
        void persistsEntityWithComputedTax() {
            LocalDate date = LocalDate.now().plusDays(5);
            SchedulePaymentRequest request = buildRequest(new BigDecimal("200.00"), date);

            ArgumentCaptor<PaymentSchedule> captor = ArgumentCaptor.forClass(PaymentSchedule.class);
            PaymentSchedule stub = PaymentSchedule.builder()
                    .originAccount("ACC-001").destinationAccount("ACC-002")
                    .amount(new BigDecimal("200.00")).tax(new BigDecimal("10.00"))
                    .scheduledDate(date).build();
            when(repository.save(captor.capture())).thenReturn(stub);

            service.schedule(request);

            PaymentSchedule captured = captor.getValue();
            assertThat(captured.getTax()).isEqualByComparingTo("10.00");
        }
    }

    @Nested
    @DisplayName("when business rules are violated")
    class BusinessRuleViolations {

        @Test
        @DisplayName("rejects scheduling for today")
        void rejectsToday() {
            SchedulePaymentRequest request = buildRequest(new BigDecimal("100.00"), LocalDate.now());
            assertThatThrownBy(() -> service.schedule(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("future");
        }

        @Test
        @DisplayName("rejects zero amount")
        void rejectsZeroAmount() {
            SchedulePaymentRequest request = buildRequest(BigDecimal.ZERO, LocalDate.now().plusDays(5));
            assertThatThrownBy(() -> service.schedule(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("greater than zero");
        }
    }

    private SchedulePaymentRequest buildRequest(BigDecimal amount, LocalDate date) {
        return new SchedulePaymentRequest("ACC-001", "ACC-002", amount, date);
    }
}
