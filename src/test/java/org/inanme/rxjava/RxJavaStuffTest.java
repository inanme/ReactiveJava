package org.inanme.rxjava;

import org.junit.Test;
import rx.Observable;
import rx.Subscriber;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class RxJavaStuffTest {

    private ExecutorService service = Executors.newFixedThreadPool(2);

    private Random random = new Random();

    @Test
    public void test1() {
        System.out.println(new Date());

        Observable.OnSubscribe<String> subscribeFunction = (s) -> {
            Subscriber subscriber = (Subscriber) s;

            List<Future<String>> futures = IntStream.range(1, 5).mapToObj(i -> (Callable<String>) () -> {
                int sleepTime = random.nextInt(10);
                TimeUnit.SECONDS.sleep(sleepTime);
                return String.format("%d wait %d", i, sleepTime);
            }).map(service::submit).collect(Collectors.toList());

            futures.forEach(f -> {
                if (!subscriber.isUnsubscribed()) {
                    try {
                        subscriber.onNext(f.get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            });
            if (!subscriber.isUnsubscribed()) {
                subscriber.onCompleted();
            }
        };

        Observable.create(subscribeFunction)
                  .subscribe((incomingValue) -> System.out.println("incomingValue " + incomingValue),
                             (error) -> System.out.println("Something went wrong" + error.getMessage()),
                             () -> System.out.println("This observable is finished"));

        System.out.println(new Date());
    }

    @Test
    public void test2() {
        List<Integer> observable = Observable.just(1, 2, 3).map(i -> i + 10).toList().toBlocking().single();
        assertThat(observable, is(Arrays.asList(11, 12, 13)));
    }

    @Test
    public void synchronousObservableExample() {
        Observable<Integer> observable = Observable.create(subscriber -> {
            PrimitiveIterator.OfInt iterator = IntStream.range(0, 5).iterator();
            if (!subscriber.isUnsubscribed()) {
                while (iterator.hasNext()) {
                    subscriber.onNext(iterator.next());
                }
            }
            if (!subscriber.isUnsubscribed()) {
                subscriber.onCompleted();
            }
        });

        observable.subscribe(s -> System.out.println("hello1-" + s));
        observable.subscribe(s -> System.out.println("hello2-" + s));
        observable.subscribe(s -> System.out.println("hello3-" + s));
    }

    @Test
    public void asynchronousObservableExample() {
        Observable<Integer> observable = Observable.create(subscriber -> {
            service.submit(() -> {
                PrimitiveIterator.OfInt iterator = IntStream.range(0, 5).iterator();
                if (!subscriber.isUnsubscribed()) {
                    while (iterator.hasNext()) {
                        subscriber.onNext(iterator.next());
                    }
                }
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onCompleted();
                }
            });
        });

        observable.subscribe(s -> System.out.println("hello1-" + s));
        //observable.subscribe(s -> System.out.println("hello2-" + s));
        //observable.subscribe(s -> System.out.println("hello3-" + s));
    }
}
