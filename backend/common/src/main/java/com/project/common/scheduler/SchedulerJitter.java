package com.project.common.scheduler;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public final class SchedulerJitter {

    private SchedulerJitter() {
    }

    public static void sleepUpTo(long maxJitterMs, String schedulerName) {
        if (maxJitterMs <= 0) {
            return;
        }
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(maxJitterMs + 1));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("{} jitter sleep interrupted", schedulerName);
        }
    }
}
