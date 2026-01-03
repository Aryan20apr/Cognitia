package com.intellidesk.cognitia.utils.exceptionHandling;

/**
 * Exception thrown when a chat thread is currently busy processing another message.
 * Client should wait for the current response to complete before sending another message.
 */
public class ThreadBusyException extends RuntimeException {
    
    private final String threadId;
    private final long queuePosition;

    public ThreadBusyException(String threadId) {
        super("Thread " + threadId + " is currently processing another message. Please wait for the response to complete.");
        this.threadId = threadId;
        this.queuePosition = 0;
    }

    public ThreadBusyException(String threadId, long queuePosition) {
        super("Thread " + threadId + " is currently processing another message. Your request is at position " + queuePosition + " in the queue.");
        this.threadId = threadId;
        this.queuePosition = queuePosition;
    }

    public String getThreadId() {
        return threadId;
    }

    public long getQueuePosition() {
        return queuePosition;
    }
}


