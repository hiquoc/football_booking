package com.project.common.inbox.handler;

import com.project.common.inbox.entity.InboxEvent;

public interface InboxEventHandler {

    boolean supports(String topic);

    void handle(InboxEvent event);
}
