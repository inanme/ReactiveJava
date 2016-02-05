package org.inanme.rxjava;

import org.junit.Test;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class RxJavaStuffTest {

    private ExecutorService service = Executors.newFixedThreadPool(3);

    private ExecutorService thread1 = Executors.newCachedThreadPool(r -> new Thread(r, "thread1"));

    private ExecutorService thread2 = Executors.newCachedThreadPool(r -> new Thread(r, "thread2"));

    private ExecutorService thread3 = Executors.newCachedThreadPool(r -> new Thread(r, "thread3"));

    private Random random = new Random();

    class Waiter implements Callable<Long> {

        private final long l;

        public Waiter(long l) {
            this.l = l;
        }

        @Override
        public Long call() {
            try {
                TimeUnit.SECONDS.sleep(l);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return l;
        }
    }

    @Test
    public void test00() {
        System.out.println(new Date());

        Observable<Long> task1 = Observable.from(service.submit(new Waiter(2)));
        Observable<Long> task2 = Observable.from(service.submit(new Waiter(3)));
        Observable<Long> task3 = Observable.from(service.submit(new Waiter(4)));
        Observable<Long> zip = Observable.zip(task1, task2, task3, (x, y, z) -> x + y + z);
        zip.subscribe(System.out::println);

        System.out.println(new Date());
    }

    @Test
    public void test0() throws InterruptedException {
        Observable<Integer> observable = Observable.create(subscriber -> {
            PrimitiveIterator.OfInt iterator = IntStream.range(0, 5).iterator();
            System.out.println(Thread.currentThread().getName() + " subs");
            if (!subscriber.isUnsubscribed()) {
                while (iterator.hasNext()) {
                    subscriber.onNext(iterator.next());
                }
            }
            if (!subscriber.isUnsubscribed()) {
                subscriber.onCompleted();
            }
        });

        observable.subscribeOn(Schedulers.from(thread1)) //
            .observeOn(Schedulers.from(thread2)) //
            .map(x -> {
                //System.out.println(Thread.currentThread().getName() + " map");
                return x + 1;
            }).subscribe(s -> System.out.println(Thread.currentThread().getName() + " observe" + s));

        observable.subscribeOn(Schedulers.from(thread1)) //
            .observeOn(Schedulers.from(thread3)) //
            .map(x -> {
                //System.out.println(Thread.currentThread().getName() + " map");
                return x * 2;
            }).subscribe(s -> System.out.println(Thread.currentThread().getName() + " observe" + s));

        TimeUnit.SECONDS.sleep(3l);
    }

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
