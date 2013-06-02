package org.github.simplefuture;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class Async {

    private Async() {
        throw new AssertionError();
    }

    public static <V> ExtendedFuture<V> future(Callable<V> callable) {
        return new FutureWrapper<V>(callable);
    }

    static void assertNotNull(Object o) {
        if (o == null) {
            throw new NullPointerException("Can't be null");
        }
    }

    static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool(new SimpleThreadFactory());

    static class SimpleThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r);
        }
    }
}
