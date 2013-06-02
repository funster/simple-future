package org.github.simplefuture;

public class FailureHolder<T extends Throwable> extends ResultHolder<T> {
    public FailureHolder(short label, T result) {
        super(label, result);
    }
}
