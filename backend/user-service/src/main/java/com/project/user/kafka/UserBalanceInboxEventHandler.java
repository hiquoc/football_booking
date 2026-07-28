package com.project.user.kafka;

import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.UserBalanceDeductionRequestedEvent;
import com.project.common.events.notification.UserBalanceRefundRequestedEvent;
import com.project.common.events.notification.WalletTopUpSucceededEvent;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.handler.InboxEventHandler;
import com.project.common.inbox.service.InboxService;
import com.project.user.service.UserBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class UserBalanceInboxEventHandler implements InboxEventHandler {
    private static final Set<String> TOPICS = Set.of(
            NotificationEventTopics.USER_BALANCE_REFUND_REQUESTED,
            NotificationEventTopics.USER_BALANCE_DEDUCTION_REQUESTED,
            NotificationEventTopics.USER_BALANCE_TOP_UP_SUCCEEDED);

    private final InboxService inboxService;
    private final UserBalanceService userBalanceService;

    @Override
    public boolean supports(String topic) {
        return TOPICS.contains(topic);
    }

    @Override
    @Transactional
    public void handle(InboxEvent event) {
        if (NotificationEventTopics.USER_BALANCE_REFUND_REQUESTED.equals(event.getTopic())) {
            userBalanceService.refund(inboxService.payload(event, UserBalanceRefundRequestedEvent.class));
            return;
        }
        if (NotificationEventTopics.USER_BALANCE_TOP_UP_SUCCEEDED.equals(event.getTopic())) {
            userBalanceService.topUp(inboxService.payload(event, WalletTopUpSucceededEvent.class));
            return;
        }
        userBalanceService.deduct(inboxService.payload(event, UserBalanceDeductionRequestedEvent.class));
    }
}
