package org.inanme.java8;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Java8StuffTest {

    private ExecutorService service = Executors.newFixedThreadPool(2);

    @Test
    public void test1() throws InterruptedException {
        Supplier<Integer> supplier = () -> {
            System.out.println("just did it!");
            return 1;
        };

        CompletableFuture<Integer> imageDataFuture = CompletableFuture.supplyAsync(supplier, service);

        imageDataFuture.thenApplyAsync(it -> {
            System.out.println("This is thenApply " + it);
            return it + 1;
        }).thenAccept(it -> System.out.println("This is thenAccept " + it));

        TimeUnit.SECONDS.sleep(3l);

        service.shutdown();

        System.out.println("killed");
    }
}
