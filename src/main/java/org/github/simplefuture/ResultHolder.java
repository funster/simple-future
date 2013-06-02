package org.github.simplefuture;

public class ResultHolder<T> {

    public ResultHolder(short label, T result) {
        this.label = label;
        this.result = result;
    }

    public final short label;
    public final T result;
}
