package com.project.user.service.impl;

import com.project.common.events.notification.PaymentFailedEvent;
import com.project.common.events.notification.PaymentSuccessEvent;
import com.project.common.dto.balance.BalanceDeductionRequest;
import com.project.common.dto.balance.BalanceDeductionResponse;
import com.project.common.events.notification.UserBalanceDeductionRequestedEvent;
import com.project.common.events.notification.UserBalanceRefundRequestedEvent;
import com.project.common.events.notification.UserBalanceUpdatedEvent;
import com.project.common.events.notification.WalletTopUpSucceededEvent;
import com.project.common.cache.CacheNames;
import com.project.common.exception.NotFoundException;
import com.project.user.entity.User;
import com.project.user.entity.UserBalanceTransaction;
import com.project.user.kafka.UserBalanceEventPublisher;
import com.project.user.repository.UserBalanceTransactionRepository;
import com.project.user.repository.UserRepository;
import com.project.user.service.UserBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
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
    private static final String TOP_UP = "TOP_UP";
    private static final String BOOKING_PAYMENT_REASON = "BOOKING_ACCOUNT_BALANCE_PAYMENT";

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
        publishBalanceUpdated(user, event.reason());
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
        publishBalanceUpdated(user, event.reason());
        publisher.publishPaymentSuccess(new PaymentSuccessEvent(
                syntheticPaymentId(operationKey),
                event.bookingId(),
                event.bookingCode(),
                event.userId(),
                user.getEmail(),
                BigDecimal.valueOf(event.amount()),
                Instant.now()));
    }

    @Override
    @Transactional
    public BalanceDeductionResponse deductSync(BalanceDeductionRequest request) {
        String operationKey = operationKey(DEDUCTION, request.reason(), request.bookingId());
        User user = userRepository.findForUpdateById(request.userId())
                .orElseThrow(() -> new NotFoundException("User not found with id " + request.userId()));
        if (transactionRepository.existsByOperationKey(operationKey)) {
            return new BalanceDeductionResponse(true, user.getBalance(), "Balance was already deducted");
        }
        if (user.getBalance() < request.amount()) {
            return new BalanceDeductionResponse(false, user.getBalance(), "Insufficient account balance");
        }
        user.setBalance(user.getBalance() - request.amount());
        transactionRepository.save(transaction(request.userId(), request.bookingId(), operationKey, DEDUCTION, request.amount(), request.reason()));
        evictUser(request.userId());
        publishBalanceUpdated(user, request.reason());
        publisher.publishPaymentSuccess(new PaymentSuccessEvent(
                syntheticPaymentId(operationKey),
                request.bookingId(),
                request.bookingCode(),
                request.userId(),
                user.getEmail(),
                BigDecimal.valueOf(request.amount()),
                Instant.now()));
        return new BalanceDeductionResponse(true, user.getBalance(), "Balance deducted");
    }

    @Override
    @Transactional
    public void topUp(WalletTopUpSucceededEvent event) {
        long amount = event.amount().longValueExact();
        String operationKey = TOP_UP + ":" + event.paymentId();
        if (transactionRepository.existsByOperationKey(operationKey)) {
            return;
        }
        User user = userRepository.findForUpdateById(event.userId())
                .orElseThrow(() -> new NotFoundException("User not found with id " + event.userId()));
        user.setBalance(user.getBalance() + amount);
        transactionRepository.save(transaction(event.userId(), null, operationKey, TOP_UP, amount, "WALLET_TOP_UP"));
        evictUser(event.userId());
        if (event.bookingId() != null && event.payableAmount() != null && event.payableAmount() > 0) {
            deductBookingPaymentAfterTopUp(user, event);
            return;
        }
        publishBalanceUpdated(user, "WALLET_TOP_UP");
    }

    private void deductBookingPaymentAfterTopUp(User user, WalletTopUpSucceededEvent event) {
        String deductionKey = operationKey(DEDUCTION, BOOKING_PAYMENT_REASON, event.bookingId());
        if (transactionRepository.existsByOperationKey(deductionKey)) {
            publishBalanceUpdated(user, "WALLET_TOP_UP");
            return;
        }
        long payableAmount = event.payableAmount();
        if (user.getBalance() < payableAmount) {
            publishBalanceUpdated(user, "WALLET_TOP_UP");
            return;
        }
        user.setBalance(user.getBalance() - payableAmount);
        transactionRepository.save(transaction(event.userId(), event.bookingId(), deductionKey, DEDUCTION, payableAmount, BOOKING_PAYMENT_REASON));
        evictUser(event.userId());
        publishBalanceUpdated(user, BOOKING_PAYMENT_REASON);
        publisher.publishPaymentSuccess(new PaymentSuccessEvent(
                syntheticPaymentId(deductionKey),
                event.bookingId(),
                event.bookingCode(),
                event.userId(),
                event.userEmail(),
                BigDecimal.valueOf(payableAmount),
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

    private void publishBalanceUpdated(User user, String reason) {
        publisher.publishBalanceUpdated(new UserBalanceUpdatedEvent(user.getId(), user.getBalance(), reason, Instant.now()));
    }

    private void evictUser(UUID userId) {
        Cache cache = cacheManager.getCache(CacheNames.USER_BY_ID);
        if (cache != null) {
            cache.evict("user:" + userId);
        }
    }
}
