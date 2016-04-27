package org.inanme.rxjava;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import org.junit.Test;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.RunnerException;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

@State(Scope.Thread)
public class RxJavaStuffTest extends Infra {

    @Test
    public void main() throws IOException, RunnerException {
        Main.main(new String[]{});
    }

    @Benchmark
    public void test000() {
        ContiguousSet<Integer> integers = ContiguousSet.create(Range.closed(1, 1000), DiscreteDomain.integers());
        integers.stream().mapToInt(Integer::intValue).sum();
    }

    @Benchmark
    public void test001() {
        Stream.iterate(0, Math::incrementExact).limit(1000).mapToInt(Integer::intValue).sum();
    }

    @Test
    public void zip3Futures() {
        Observable<Long> task1 = Observable.from(ioPool.submit(new Waiter(2)));
        Observable<Long> task2 = Observable.from(ioPool.submit(new Waiter(3)));
        Observable<Long> task3 = Observable.from(ioPool.submit(new Waiter(4)));
        Observable<Long> zip = Observable.zip(task1, task2, task3, (x, y, z) -> x + y + z);
        zip.subscribe(System.out::println);
    }

    @Test
    public void test0() {
        Observable<Integer> observable = Observable.create(subscriber -> {
            PrimitiveIterator.OfInt iterator = IntStream.range(1, 3).iterator();
            log("subs");
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
                      log("map");
                      return x + 1;
                  }).subscribe(s -> log("observe " + s));

        observable.subscribeOn(Schedulers.from(thread1)) //
                  .observeOn(Schedulers.from(thread3)) //
                  .map(x -> {
                      log("map");
                      return x * 2;
                  }).subscribe(s -> log("observe " + s));

        giveMeTime(3l);
    }

    @Test
    public void test1() {
        Observable.OnSubscribe<String> subscribeFunction = (s) -> {

            List<Future<String>> futures = IntStream.range(1, 5).mapToObj(i -> (Callable<String>) () -> {
                int sleepTime = random.nextInt(10);
                TimeUnit.SECONDS.sleep(sleepTime);
                return String.format("%d wait %d", i, sleepTime);
            }).map(ioPool::submit).collect(Collectors.toList());

            futures.forEach(f -> {
                if (!s.isUnsubscribed()) {
                    try {
                        s.onNext(f.get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            });
            if (!s.isUnsubscribed()) {
                s.onCompleted();
            }
        };

        Observable.create(subscribeFunction).subscribe((incomingValue) -> log("incomingValue " + incomingValue),
                                                       (error) -> log("Something went wrong" + error.getMessage()),
                                                       () -> log("This observable is finished"));
    }

    @Test
    public void primitiveProcessing() {
        Observable<Integer> just = Observable.just(1, 2, 3);

        List<Integer> single10 = just.map(i -> i + 10).toList().toBlocking().single();
        assertThat(single10, is(Arrays.asList(11, 12, 13)));

        List<Integer> single20 = just.map(i -> i + 20).toList().toBlocking().single();
        assertThat(single20, is(Arrays.asList(21, 22, 23)));

        List<Integer> collect = IntStream.range(0, 100).mapToObj(Integer::valueOf).collect(Collectors.toList());

        Integer single =
            Observable.from(collect).take(11).filter(x -> x % 2 == 0).map(x -> x * 2).reduce(0, (x, y) -> x + y)
                      .toBlocking().single();

        log(single);
    }

    @Test
    public void synchronousObservableExample() {
        Observable<Integer> observable = Observable.create(subscriber -> {
            PrimitiveIterator.OfInt iterator = IntStream.range(0, 3).iterator();
            if (!subscriber.isUnsubscribed()) {
                while (iterator.hasNext()) {
                    subscriber.onNext(iterator.next());
                }
            }
            if (!subscriber.isUnsubscribed()) {
                subscriber.onCompleted();
            }
        });
        observable.subscribe(i -> log("hello1-" + i));
        observable.forEach(i -> log("hello2-" + i));
        observable.filter(i -> i % 2 == 0).forEach(i -> log("hello3-" + i));
    }

    @Test
    public void asynchronousObservableExample() throws InterruptedException {
        Observable<Integer> observable = Observable.create(subscriber -> {
            ioPool.submit(() -> {
                PrimitiveIterator.OfInt iterator = IntStream.range(0, 3).iterator();
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

        observable.subscribe(s -> log("hello1-" + s));
        //observable.subscribe(s -> log("hello2-" + s));
        //observable.subscribe(s -> log("hello3-" + s));
    }

    @Test
    public void none() {

        Observable.just(1, 2, 3)
                  //Asynchronously subscribes Observers to this Observable
                  .subscribeOn(Schedulers.from(thread1)).observeOn(Schedulers.from(thread2))
                  .doOnNext(x -> log(String.format("I  :%s:%d", Thread.currentThread().getName(), x)))

                  .observeOn(Schedulers.from(thread3))
                  .doOnNext(x -> log(String.format("II :%s:%d", Thread.currentThread().getName(), x))).map(x -> x * 2)

                  //.observeOn(Schedulers.from(thread3))
                  .doOnNext(x -> log(String.format("III:%s:%d", Thread.currentThread().getName(), x)))
                  .subscribe(x -> log(String.format("IV :%s:%d", Thread.currentThread().getName(), x)));

        giveMeTime(3l);
    }

    @Test
    public void interval() {
        Observable.interval(1, TimeUnit.SECONDS, ioScheduler).observeOn(ioScheduler).subscribe(s -> log(s));

        giveMeTime(3l);
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
