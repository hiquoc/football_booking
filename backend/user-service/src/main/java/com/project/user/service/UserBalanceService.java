package com.project.user.service;

import com.project.common.dto.balance.BalanceDeductionRequest;
import com.project.common.dto.balance.BalanceDeductionResponse;
import com.project.common.events.notification.UserBalanceDeductionRequestedEvent;
import com.project.common.events.notification.UserBalanceRefundRequestedEvent;
import com.project.common.events.notification.WalletTopUpSucceededEvent;

public interface UserBalanceService {
    void refund(UserBalanceRefundRequestedEvent event);
    void deduct(UserBalanceDeductionRequestedEvent event);
    BalanceDeductionResponse deductSync(BalanceDeductionRequest request);
    void topUp(WalletTopUpSucceededEvent event);
}
