package com.safepay.schedule.service;

import com.safepay.schedule.domain.entity.PaymentSchedule;
import com.safepay.schedule.domain.exception.BusinessException;
import com.safepay.schedule.domain.policy.TaxPolicy;
import com.safepay.schedule.dto.SchedulePaymentRequest;
import com.safepay.schedule.dto.SchedulePaymentResponse;
import com.safepay.schedule.repository.PaymentScheduleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PaymentScheduleService {

    private final PaymentScheduleRepository repository;

    public PaymentScheduleService(PaymentScheduleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SchedulePaymentResponse> findAll() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(SchedulePaymentResponse::from)
                .toList();
    }

    @Transactional
    public SchedulePaymentResponse schedule(SchedulePaymentRequest request) {
        validateBusinessRules(request.amount(), request.scheduledDate());

        BigDecimal tax = TaxPolicy.compute(request.amount(), request.scheduledDate());

        PaymentSchedule entity = PaymentSchedule.builder()
                .originAccount(request.originAccount())
                .destinationAccount(request.destinationAccount())
                .amount(request.amount())
                .tax(tax)
                .scheduledDate(request.scheduledDate())
                .build();

        PaymentSchedule saved = repository.save(entity);
        return SchedulePaymentResponse.from(saved);
    }

    private void validateBusinessRules(BigDecimal amount, LocalDate scheduledDate) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Transfer amount must be greater than zero.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!scheduledDate.isAfter(LocalDate.now())) {
            throw new BusinessException("Scheduled date must be strictly in the future.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
