package org.github.simplefuture;

import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SuppressWarnings("unused")
public class FutureWrapper<V> implements Future<V> {

    private FutureSuccess<V> success;
    private FutureFailure failure;
    private final FutureTask<V> futureTask;

    // --- RESULTS
    private volatile Throwable throwable;
    private volatile V result;

    // --- FLAGS
    private volatile Status status; // with success or not

    enum Status {
        not_defined, success, fail
    }

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    private final Object finalizerGuard = new Object() {
        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            if (!LazyExecutor.EXECUTOR_SERVICE.isShutdown()) {
                LazyExecutor.EXECUTOR_SERVICE.shutdown();
            }
        }
    };

    private FutureWrapper(final Callable<V> callable) {
        Callable<V> wrap = new Callable<V>() {
            @Override
            public V call() throws Exception {
                try {
                    result = callable.call();
                    status = Status.success;
                    notifyIfSuccess();
                    return result;
                } catch (Throwable e) {
                    throwable = e;
                    status = Status.fail;
                    notifyIfFailure();
                    return null;
                }
            }
        };
        futureTask = new FutureTask<V>(wrap);
        submit(futureTask);
    }

    // todo: synchronize this in promise
    void setSuccess(V result) {
        writeLock.lock();
        try {
            this.result = result;
            status = Status.success;
        } finally {
            writeLock.lock();
        }
    }

    // todo: synchronize this in promise
    void setFailure(Throwable throwable) {
        writeLock.lock();
        try {
            this.throwable = throwable;
            status = Status.fail;
        } finally {
            writeLock.unlock();
        }
    }

    public static <V> FutureWrapper<V> future(Callable<V> callable) {
        return new FutureWrapper<V>(callable);
    }

    private void notifyIfSuccess() {
        if (status == Status.success) {
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
        if (status == Status.fail) {
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


    //----- FEATURE METHODS -----
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        readLock.lock();
        try {
            return isUndefined() && futureTask.cancel(mayInterruptIfRunning);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isCancelled() {
        return futureTask.isCancelled();
    }

    private boolean isUndefined() {
        return status == Status.not_defined;
    }

    @Override
    public boolean isDone() {
        readLock.lock();
        try {
            return !isUndefined() || futureTask.isDone();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        readLock.lock();
        try {
            if (isUndefined()) {
                return futureTask.get();
            } else {
                switch (status) {
                    case success:
                        return result;
                    default:
                        return null;
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        readLock.lock();
        try {
            if (isUndefined()) {
                return futureTask.get(timeout, unit);
            } else {
                switch (status) {
                    case success:
                        return result;
                    default:
                        return null;
                }
            }
        } finally {
            readLock.unlock();
        }
    }
    //----- FEATURE METHODS -----

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

    static void assertNotNull(Object o) {
        if (o == null) {
            throw new NullPointerException("Can't be null");
        }
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
}
