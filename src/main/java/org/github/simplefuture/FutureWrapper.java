package org.github.simplefuture;

import java.util.concurrent.*;

@SuppressWarnings("unused")
public class FutureWrapper<V> implements Future<V> {

    private FutureSuccess<V> success;
    private FutureFailure failure;
    private final FutureTask<V> futureTask;
    private Throwable throwable;
    private volatile boolean successInvoked;
    private volatile boolean failureInvoked;

    private final Object finalizerGuard = new Object() {
        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            LazyExecutor.EXECUTOR_SERVICE.shutdown();
        }
    };

    private FutureWrapper(final Callable<V> callable) {
        Callable<V> wrap = new Callable<V>() {
            @Override
            public V call() throws Exception {
                try {
                    return callable.call();
                } catch (Throwable e) {
                    throwable = e;
                    notifyIfFailure();
                    return null;
                } finally {
                    notifyIfSuccess();
                }
            }
        };
        futureTask = new FutureTask<V>(wrap);
        submit(futureTask);
    }

    public static <V> FutureWrapper<V> future(Callable<V> callable) {
        return new FutureWrapper<V>(callable);
    }

    private void notifyIfSuccess() {
        if (success != null && !successInvoked) {
            successInvoked = true;
            LazyExecutor.EXECUTOR_SERVICE.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        success.apply(futureTask.get());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void notifyIfFailure() {
        if (throwable != null && !failureInvoked) {
            failureInvoked = true;
            LazyExecutor.EXECUTOR_SERVICE.submit(new Runnable() {
                @Override
                public void run() {
                    failure.apply(throwable);
                }
            });
        }
    }

    private static void submit(Runnable runnable) {
        LazyExecutor.EXECUTOR_SERVICE.submit(runnable);
    }

    static class LazyExecutor {
        private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool(new DaemonThreadFactory());
    }

    static class DaemonThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return futureTask.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return futureTask.isCancelled();
    }

    @Override
    public boolean isDone() {
        return futureTask.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return futureTask.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return futureTask.get(timeout, unit);
    }

    public FutureWrapper<V> onSuccess(FutureSuccess<V> success) {
        assertNotNull(success);
        this.success = success;
        notifyIfSuccess();
        return this;
    }

    public FutureWrapper<V> onFailure(FutureFailure failure) {
        assertNotNull(failure);
        this.failure = failure;
        notifyIfFailure();
        return this;
    }

    public interface FutureSuccess<T> {
        void apply(T result);
    }

    public interface FutureFailure {
        void apply(Throwable throwable);
    }

    static void assertNotNull(Object o) {
        if (o == null) {
            throw new NullPointerException("Can't be null");
        }
    }
}
