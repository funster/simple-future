package org.github.simplefuture;

public interface Promise<V> {

    public void success(V result);

    public void failure(Throwable throwable);
}
