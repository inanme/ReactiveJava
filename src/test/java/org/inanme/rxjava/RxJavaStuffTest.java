package org.inanme.rxjava;

import com.google.common.collect.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.RunnerException;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@State(Scope.Thread)
public class RxJavaStuffTest {

    @Rule
    public TestName name = new TestName();

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

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

    public void test() throws IOException, RunnerException {
        Main.main(new String[]{});
    }

    @Before
    public void start() {
        System.out.printf("Test %s started: %s\n", name.getMethodName(), sdf.format(new Date()));
    }

    @After
    public void finish() {
        System.out.printf("Test %s ended  : %s\n", name.getMethodName(), sdf.format(new Date()));
    }

    @Benchmark
    public void test000() {
        ContiguousSet<Integer> integers = ContiguousSet.create(Range.closed(1, 1000), DiscreteDomain.integers());
        integers.stream().mapToInt(Integer::intValue).sum();
    }

    @Test
    public void test001() {
        Observable<Long> task1 = Observable.from(service.submit(new Waiter(2)));
        Observable<Long> task2 = Observable.from(service.submit(new Waiter(3)));
        Observable<Long> task3 = Observable.from(service.submit(new Waiter(4)));
        Observable<Long> zip = Observable.zip(task1, task2, task3, (x, y, z) -> x + y + z);
        zip.subscribe(System.out::println);
    }

    @Test
    public void test0() throws InterruptedException {
        Observable<Integer> observable = Observable.create(subscriber -> {
            PrimitiveIterator.OfInt iterator = IntStream.range(1, 5).iterator();
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
                }).subscribe(s -> System.out.println(Thread.currentThread().getName() + " observe " + s));

        observable.subscribeOn(Schedulers.from(thread1)) //
                .observeOn(Schedulers.from(thread3)) //
                .map(x -> {
                    //System.out.println(Thread.currentThread().getName() + " map");
                    return x * 2;
                }).subscribe(s -> System.out.println(Thread.currentThread().getName() + " observe " + s));

        TimeUnit.SECONDS.sleep(3l);
    }

    @Test
    public void test1() {
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
    }

    @Test
    public void primitiveProcessing() {
        Observable<Integer> just = Observable.just(1, 2, 3);

        List<Integer> single10 = just.map(i -> i + 10).toList().toBlocking().single();
        assertThat(single10, is(Arrays.asList(11, 12, 13)));

        List<Integer> single20 = just.map(i -> i + 20).toList().toBlocking().single();
        assertThat(single20, is(Arrays.asList(21, 22, 23)));

        List<Integer> collect = IntStream.range(0, 100).mapToObj(Integer::valueOf).collect(Collectors.toList());

        Integer single = Observable.from(collect)
                .take(11)
                .filter(x -> x % 2 == 0)
                .map(x -> x * 2)
                .reduce(0, (x, y) -> x + y)
                .toBlocking().single();

        System.out.println(single);
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
        observable.subscribe(i -> System.out.println("hello1-" + i));
        observable.forEach(i -> System.out.println("hello2-" + i));
        observable.filter(i -> i % 2 == 0).forEach(i -> System.out.println("hello3-" + i));
    }

    @Test
    public void asynchronousObservableExample() throws InterruptedException {
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

    @Test
    public void none() throws InterruptedException {

        Observable.just(1, 2, 3)
                //Asynchronously subscribes Observers to this Observable
                .subscribeOn(Schedulers.from(thread1))
                .observeOn(Schedulers.from(thread2))
                .doOnNext(x -> System.err.println(String.format("I  :%s:%d", Thread.currentThread().getName(), x)))

                .observeOn(Schedulers.from(thread3))
                .doOnNext(x -> System.err.println(String.format("II :%s:%d", Thread.currentThread().getName(), x)))
                .map(x -> x * 2)

                //.observeOn(Schedulers.from(thread3))
                .doOnNext(x -> System.err.println(String.format("III:%s:%d", Thread.currentThread().getName(), x)))
                .subscribe(x -> System.err.println(String.format("IV :%s:%d", Thread.currentThread().getName(), x)));

        TimeUnit.SECONDS.sleep(3l);

    }

    @Test
    public void interval() throws InterruptedException {
        Observable
                .interval(1, TimeUnit.SECONDS, Schedulers.from(service))
                .observeOn(Schedulers.from(service))
                .subscribe(s -> System.out.println(Thread.currentThread().getName()));

        TimeUnit.SECONDS.sleep(3l);
    }

    @Test
    public void mergeConcatZip() {
        List<Integer> list1 = Arrays.asList(1, 2, 3, 4);
        List<Integer> list2 = Arrays.asList(5, 6, 7, 8, 9);
        Observable.from(Iterables.concat(list1, list2));
        Observable.from(list1).mergeWith(Observable.from(list2));
        Observable.zip(Observable.from(list1), Observable.from(list2), (x, y) -> x + y);
        Observable.from(list1).concatWith(Observable.from(list2));
        Observable.merge(Observable.from(list1), Observable.from(list2)).forEach(System.out::println);
    }

}
