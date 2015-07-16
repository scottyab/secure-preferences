package com.securepreferences.sample.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Simple performance timer
 * Created by scottab on 15/07/15.
 */
public class TickTock {
    private long startTime;
    private long endTime;
    private State state;

    public void tic() {
        this.state = State.STARTED;

        this.startTime = System.currentTimeMillis();
    }

    public long toc() {
        this.endTime = System.currentTimeMillis();

        if (this.state == State.STARTED) {
            this.state = State.STOPPED;
            return this.endTime - this.startTime;
        }
        return -1L;
    }

    public static String formatDuration(long durationMillis) {
        return new SimpleDateFormat("mm:ss:SSS")
                .format(new Date(durationMillis));
    }

    private static enum State {
        STOPPED, STARTED;
    }
}
