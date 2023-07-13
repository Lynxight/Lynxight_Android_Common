package com.lynxight.common.utils;

import java.time.Duration;
import java.time.LocalDateTime;

public class StopWatch {
    private LocalDateTime startTime;
    private Duration totalRunTime = Duration.ZERO;

    public void start() {
        if (!isRunning()) {
            startTime = LocalDateTime.now();
        }
    }

    private boolean isRunning() {
        return startTime != null;
    }

    public void stop() {
        if (isRunning()) {
            Duration runTime = Duration.between(startTime, LocalDateTime.now());
            totalRunTime = totalRunTime.plus(runTime);
            startTime = null;
        }
    }

    public void resetTotalTime() {
        if (isRunning()) {
            startTime = LocalDateTime.now();
        }
        totalRunTime = Duration.ZERO;
    }

    public void resetAll() {
        stop();
        totalRunTime = Duration.ZERO;
    }

    public Duration getElapsedTime() {
        Duration currentDuration = Duration.ZERO;
        currentDuration = currentDuration.plus(totalRunTime);
        if (isRunning()) {
            Duration runTime = Duration.between(startTime, LocalDateTime.now());
            currentDuration = currentDuration.plus(runTime);
        }
        return currentDuration;
    }
}