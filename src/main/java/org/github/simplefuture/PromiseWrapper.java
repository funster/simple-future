package org.github.simplefuture;

class PromiseWrapper<V> implements Promise<V> {

    final FutureWrapper<V> future;

    PromiseWrapper(FutureWrapper<V> future) {
        this.future = future;
    }

    @Override
    public void success(V result) {
        future.setSuccess(result);
    }

    @Override
    public void failure(Throwable throwable) {
        future.setFailure(throwable);
    }
}
