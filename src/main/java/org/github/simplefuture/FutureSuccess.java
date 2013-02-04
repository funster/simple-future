package org.github.simplefuture;

public interface FutureSuccess<T> {
    void apply(T result);
}
