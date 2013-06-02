package org.github.simplefuture;

import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.github.simplefuture.Async.assertNotNull;

class FutureWrapper<V> implements ExtendedFuture<V> {

    private FutureSuccess<V> success;
    private FutureFailure failure;
    private final FutureTask<V> futureTask;
    private final Promise<V> promise = new PromiseWrapper<V>(this);

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

    FutureWrapper(final Callable<V> callable) {
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

    void setSuccess(V result) {
        writeLock.lock();
        try {
            futureTask.cancel(true);
            this.result = result;
            status = Status.success;
        } finally {
            writeLock.lock();
        }
    }

    void setFailure(Throwable throwable) {
        writeLock.lock();
        try {
            futureTask.cancel(true);
            this.throwable = throwable;
            status = Status.fail;
        } finally {
            writeLock.unlock();
        }
    }

    private void notifyIfSuccess() {
        if (status == Status.success) {
            Async.EXECUTOR_SERVICE.submit(new Runnable() {
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
            Async.EXECUTOR_SERVICE.submit(new Runnable() {
                @Override
                public void run() {
                    failure.apply(throwable);
                }
            });
        }
    }

    private static void submit(Runnable runnable) {
        Async.EXECUTOR_SERVICE.submit(runnable);
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
    @Override
    public FutureWrapper<V> onSuccess(FutureSuccess<V> success) {
        assertNotNull(success);
        this.success = success;
        notifyIfSuccess();
        return this;
    }

    @Override
    public FutureWrapper<V> onFailure(FutureFailure failure) {
        assertNotNull(failure);
        this.failure = failure;
        notifyIfFailure();
        return this;
    }

    @Override
    public Promise<V> getPromise() {
        return promise;
    }
}
