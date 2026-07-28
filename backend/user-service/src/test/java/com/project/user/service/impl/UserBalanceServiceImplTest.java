package com.project.user.service.impl;

import com.project.common.enums.UserType;
import com.project.common.cache.CacheNames;
import com.project.common.dto.balance.BalanceDeductionRequest;
import com.project.common.dto.balance.BalanceDeductionResponse;
import com.project.common.events.notification.PaymentFailedEvent;
import com.project.common.events.notification.PaymentSuccessEvent;
import com.project.common.events.notification.UserBalanceDeductionRequestedEvent;
import com.project.common.events.notification.UserBalanceRefundRequestedEvent;
import com.project.common.events.notification.WalletTopUpSucceededEvent;
import com.project.user.entity.User;
import com.project.user.entity.UserBalanceTransaction;
import com.project.user.kafka.UserBalanceEventPublisher;
import com.project.user.repository.UserBalanceTransactionRepository;
import com.project.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBalanceServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBalanceTransactionRepository transactionRepository;

    @Mock
    private UserBalanceEventPublisher publisher;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache userCache;

    @InjectMocks
    private UserBalanceServiceImpl service;

    @Test
    void refundCreditsBalanceAndStoresIdempotencyTransaction() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        User user = user(userId, 10000L);
        UserBalanceRefundRequestedEvent event = new UserBalanceRefundRequestedEvent(
                userId, 5000L, bookingId, "BK-1", "BOOKING_CANCEL_REFUND", Instant.now());

        when(transactionRepository.existsByOperationKey("REFUND:BOOKING_CANCEL_REFUND:" + bookingId)).thenReturn(false);
        when(userRepository.findForUpdateById(userId)).thenReturn(Optional.of(user));
        when(cacheManager.getCache(CacheNames.USER_BY_ID)).thenReturn(userCache);

        service.refund(event);

        assertEquals(15000L, user.getBalance());
        ArgumentCaptor<UserBalanceTransaction> transactionCaptor = ArgumentCaptor.forClass(UserBalanceTransaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        verify(userCache).evict("user:" + userId);
        assertEquals("REFUND", transactionCaptor.getValue().getType());
        assertEquals(5000L, transactionCaptor.getValue().getAmount());
    }

    @Test
    void duplicateRefundDoesNotModifyBalance() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UserBalanceRefundRequestedEvent event = new UserBalanceRefundRequestedEvent(
                userId, 5000L, bookingId, "BK-1", "BOOKING_CANCEL_REFUND", Instant.now());

        when(transactionRepository.existsByOperationKey("REFUND:BOOKING_CANCEL_REFUND:" + bookingId)).thenReturn(true);

        service.refund(event);

        verify(userRepository, never()).findForUpdateById(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deductDebitsBalanceAndPublishesPaymentSuccess() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        User user = user(userId, 20000L);
        UserBalanceDeductionRequestedEvent event = new UserBalanceDeductionRequestedEvent(
                userId, 12000L, bookingId, "BK-2", "BOOKING_ACCOUNT_BALANCE_PAYMENT", Instant.now());

        when(transactionRepository.existsByOperationKey("DEDUCTION:BOOKING_ACCOUNT_BALANCE_PAYMENT:" + bookingId)).thenReturn(false);
        when(userRepository.findForUpdateById(userId)).thenReturn(Optional.of(user));
        when(cacheManager.getCache(CacheNames.USER_BY_ID)).thenReturn(userCache);

        service.deduct(event);

        assertEquals(8000L, user.getBalance());
        verify(userCache).evict("user:" + userId);
        verify(transactionRepository).save(any(UserBalanceTransaction.class));
        ArgumentCaptor<PaymentSuccessEvent> eventCaptor = ArgumentCaptor.forClass(PaymentSuccessEvent.class);
        verify(publisher).publishPaymentSuccess(eventCaptor.capture());
        assertEquals(bookingId, eventCaptor.getValue().bookingId());
        assertEquals(new BigDecimal("12000"), eventCaptor.getValue().amount());
    }

    @Test
    void insufficientBalanceDoesNotDebitAndPublishesPaymentFailure() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        User user = user(userId, 1000L);
        UserBalanceDeductionRequestedEvent event = new UserBalanceDeductionRequestedEvent(
                userId, 12000L, bookingId, "BK-2", "BOOKING_ACCOUNT_BALANCE_PAYMENT", Instant.now());

        when(transactionRepository.existsByOperationKey("DEDUCTION:BOOKING_ACCOUNT_BALANCE_PAYMENT:" + bookingId)).thenReturn(false);
        when(userRepository.findForUpdateById(userId)).thenReturn(Optional.of(user));

        service.deduct(event);

        assertEquals(1000L, user.getBalance());
        verify(transactionRepository, never()).save(any());
        ArgumentCaptor<PaymentFailedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(publisher).publishPaymentFailed(eventCaptor.capture());
        assertEquals("Insufficient account balance", eventCaptor.getValue().reason());
    }

    @Test
    void deductSyncDebitsBalanceAndPublishesPaymentSuccessForRecovery() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        User user = user(userId, 20000L);
        BalanceDeductionRequest request = new BalanceDeductionRequest(
                userId, 12000L, bookingId, "BK-2", "BOOKING_ACCOUNT_BALANCE_PAYMENT");

        when(transactionRepository.existsByOperationKey("DEDUCTION:BOOKING_ACCOUNT_BALANCE_PAYMENT:" + bookingId)).thenReturn(false);
        when(userRepository.findForUpdateById(userId)).thenReturn(Optional.of(user));
        when(cacheManager.getCache(CacheNames.USER_BY_ID)).thenReturn(userCache);

        BalanceDeductionResponse response = service.deductSync(request);

        assertEquals(8000L, user.getBalance());
        assertEquals(8000L, response.balance());
        verify(userCache).evict("user:" + userId);
        verify(transactionRepository).save(any(UserBalanceTransaction.class));
        ArgumentCaptor<PaymentSuccessEvent> eventCaptor = ArgumentCaptor.forClass(PaymentSuccessEvent.class);
        verify(publisher).publishPaymentSuccess(eventCaptor.capture());
        assertEquals(bookingId, eventCaptor.getValue().bookingId());
        assertEquals(new BigDecimal("12000"), eventCaptor.getValue().amount());
    }

    @Test
    void bookingTopUpDeductsBookingFeeAndPublishesPaymentSuccess() {
        UUID paymentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        User user = user(userId, 1000L);
        WalletTopUpSucceededEvent event = new WalletTopUpSucceededEvent(
                paymentId, bookingId, "BK-3", userId, "user@example.com",
                new BigDecimal("20000"), 12000L, "VND", Instant.now());

        when(transactionRepository.existsByOperationKey("TOP_UP:" + paymentId)).thenReturn(false);
        when(transactionRepository.existsByOperationKey("DEDUCTION:BOOKING_ACCOUNT_BALANCE_PAYMENT:" + bookingId)).thenReturn(false);
        when(userRepository.findForUpdateById(userId)).thenReturn(Optional.of(user));
        when(cacheManager.getCache(CacheNames.USER_BY_ID)).thenReturn(userCache);

        service.topUp(event);

        assertEquals(9000L, user.getBalance());
        ArgumentCaptor<UserBalanceTransaction> transactionCaptor = ArgumentCaptor.forClass(UserBalanceTransaction.class);
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        List<UserBalanceTransaction> transactions = transactionCaptor.getAllValues();
        assertEquals("TOP_UP", transactions.get(0).getType());
        assertEquals(20000L, transactions.get(0).getAmount());
        assertEquals("DEDUCTION", transactions.get(1).getType());
        assertEquals(12000L, transactions.get(1).getAmount());
        ArgumentCaptor<PaymentSuccessEvent> eventCaptor = ArgumentCaptor.forClass(PaymentSuccessEvent.class);
        verify(publisher).publishPaymentSuccess(eventCaptor.capture());
        assertEquals(bookingId, eventCaptor.getValue().bookingId());
        assertEquals(new BigDecimal("12000"), eventCaptor.getValue().amount());
    }

    private User user(UUID id, long balance) {
        return User.builder()
                .id(id)
                .email("user@example.com")
                .fullName("Test User")
                .userType(UserType.CLIENT)
                .balance(balance)
                .build();
    }
}
