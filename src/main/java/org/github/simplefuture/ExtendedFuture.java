package org.github.simplefuture;

import java.util.concurrent.Future;

public interface ExtendedFuture<V> extends Future<V> {

    ExtendedFuture<V> onSuccess(FutureSuccess<V> success);

    ExtendedFuture<V> onFailure(FutureFailure failure);

    Promise<V> getPromise();
}
