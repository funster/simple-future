package org.github.simplefuture.example;

import org.github.simplefuture.FutureSuccess;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.github.simplefuture.FutureWrapper.future;

public class EntryPoint {

    public static void main(String... args) throws Exception {
        future(new Callable<String>() {
            @Override
            public String call() throws Exception {
                System.out.println("before");
                TimeUnit.SECONDS.sleep(1);
                return "test432";
            }
        }).onSuccess(new FutureSuccess<String>() {
            @Override
            public void apply(String result) {
                System.out.println("success: " + result);
            }
        });
        TimeUnit.SECONDS.sleep(3);
    }
}
