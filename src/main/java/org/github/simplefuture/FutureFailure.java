package org.github.simplefuture;

public interface FutureFailure {
    void apply(Throwable throwable);
}
