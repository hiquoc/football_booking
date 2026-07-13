package com.project.user.service;

import com.project.common.events.notification.UserBalanceDeductionRequestedEvent;
import com.project.common.events.notification.UserBalanceRefundRequestedEvent;

public interface UserBalanceService {
    void refund(UserBalanceRefundRequestedEvent event);
    void deduct(UserBalanceDeductionRequestedEvent event);
}
