package com.project.user.service.impl;

import com.project.common.events.notification.PaymentFailedEvent;
import com.project.common.events.notification.PaymentSuccessEvent;
import com.project.common.events.notification.UserBalanceDeductionRequestedEvent;
import com.project.common.events.notification.UserBalanceRefundRequestedEvent;
import com.project.common.cache.CacheNames;
import com.project.common.exception.NotFoundException;
import com.project.user.entity.User;
import com.project.user.entity.UserBalanceTransaction;
import com.project.user.kafka.UserBalanceEventPublisher;
import com.project.user.repository.UserBalanceTransactionRepository;
import com.project.user.repository.UserRepository;
import com.project.user.service.UserBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserBalanceServiceImpl implements UserBalanceService {

    private static final String REFUND = "REFUND";
    private static final String DEDUCTION = "DEDUCTION";

    private final UserRepository userRepository;
    private final UserBalanceTransactionRepository transactionRepository;
    private final UserBalanceEventPublisher publisher;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public void refund(UserBalanceRefundRequestedEvent event) {
        String operationKey = operationKey(REFUND, event.reason(), event.bookingId());
        if (transactionRepository.existsByOperationKey(operationKey)) {
            return;
        }
        User user = userRepository.findForUpdateById(event.userId())
                .orElseThrow(() -> new NotFoundException("User not found with id " + event.userId()));
        user.setBalance(user.getBalance() + event.amount());
        transactionRepository.save(transaction(event.userId(), event.bookingId(), operationKey, REFUND, event.amount(), event.reason()));
        evictUser(event.userId());
    }

    @Override
    @Transactional
    public void deduct(UserBalanceDeductionRequestedEvent event) {
        String operationKey = operationKey(DEDUCTION, event.reason(), event.bookingId());
        if (transactionRepository.existsByOperationKey(operationKey)) {
            return;
        }
        User user = userRepository.findForUpdateById(event.userId())
                .orElseThrow(() -> new NotFoundException("User not found with id " + event.userId()));
        if (user.getBalance() < event.amount()) {
            publisher.publishPaymentFailed(new PaymentFailedEvent(
                    syntheticPaymentId(operationKey),
                    event.bookingId(),
                    event.bookingCode(),
                    event.userId(),
                    user.getEmail(),
                    BigDecimal.valueOf(event.amount()),
                    "Insufficient account balance",
                    Instant.now()));
            return;
        }
        user.setBalance(user.getBalance() - event.amount());
        transactionRepository.save(transaction(event.userId(), event.bookingId(), operationKey, DEDUCTION, event.amount(), event.reason()));
        evictUser(event.userId());
        publisher.publishPaymentSuccess(new PaymentSuccessEvent(
                syntheticPaymentId(operationKey),
                event.bookingId(),
                event.bookingCode(),
                event.userId(),
                user.getEmail(),
                BigDecimal.valueOf(event.amount()),
                Instant.now()));
    }

    private UserBalanceTransaction transaction(UUID userId, UUID bookingId, String operationKey, String type, long amount, String reason) {
        return UserBalanceTransaction.builder()
                .userId(userId)
                .bookingId(bookingId)
                .operationKey(operationKey)
                .type(type)
                .amount(amount)
                .reason(reason)
                .build();
    }

    private String operationKey(String type, String reason, UUID bookingId) {
        return type + ":" + reason + ":" + bookingId;
    }

    private UUID syntheticPaymentId(String operationKey) {
        return UUID.nameUUIDFromBytes(operationKey.getBytes(StandardCharsets.UTF_8));
    }

    private void evictUser(UUID userId) {
        var cache = cacheManager.getCache(CacheNames.USER_BY_ID);
        if (cache != null) {
            cache.evict("user:" + userId);
        }
    }
}
