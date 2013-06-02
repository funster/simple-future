package org.github.simplefuture;

import org.testng.annotations.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.github.simplefuture.Async.future;

public class BaseTests {

    @Test
    public void testSimpleExample() throws Exception {
        ExtendedFuture<String> future = future(new Callable<String>() {
            @Override
            public String call() throws Exception {
                long pause = 5;
                TimeUnit.SECONDS.sleep(pause);
                return pause + " seconds result";
            }
        });
        Promise<String> promise = future.getPromise();
        TimeUnit.SECONDS.sleep(1);
        promise.success("from promise");
        org.testng.Assert.assertEquals(future.get(), "from promise");
    }
}
