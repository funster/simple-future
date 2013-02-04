package org.github.simplefuture;

@SuppressWarnings("unused")
public class Promise<V> {

    public void success(V result) {

    }

    public void failure(Throwable throwable) {

    }

    public FutureWrapper<V> future() {
        return null;
    }
}
