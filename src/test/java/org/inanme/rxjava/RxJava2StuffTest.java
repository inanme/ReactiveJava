package org.inanme.rxjava;

import com.google.common.collect.Iterables;
import io.reactivex.*;
import io.reactivex.subjects.Subject;
import org.junit.Test;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class RxJava2StuffTest extends Infra {

    @Test
    public void zip() {
        Observable<Long> task1 = Observable.fromFuture(future(2));
        Observable<Long> task2 = Observable.fromFuture(future(3));
        Observable<Long> task3 = Observable.fromFuture(future(4));
        Observable<Long> zip = Observable.zip(task1, task2, task3, (x, y, z) -> x + y + z);
        zip.subscribe(this::log);
    }

    @Test
    public void newStuff() {
        Observable.fromFuture(future(1)).toList().blockingGet();
        Single.fromFuture(future(1)).blockingGet();
        Maybe.fromFuture(future(1)).blockingGet();
        Flowable.fromFuture(future(1)).toList().blockingGet();
        Completable.fromFuture(future(1)).blockingGet();
        Subject.fromFuture(future(1)).toList().blockingGet();
    }

    @Test
    public void defer() {
        Observable.<Integer>defer(() -> observer -> {
            _123.forEach(observer::onNext);
            observer.onComplete();
        });
    }

    @Test
    public void experiments() {
        Observable.create(e -> {
            _123.forEach(l -> {
                if (!e.isDisposed()) {
                    log("pushed -> " + l);
                    e.onNext(l);
                }
            });
            e.setCancellable(() -> log("cancel"));
            e.onComplete();
        }).take(1).forEach(this::log);

    }

    @Test
    public void test0() {
        Observable<Integer> observable = Observable.create(subscriber -> {
            PrimitiveIterator.OfInt iterator = IntStream.range(1, 3).iterator();
            log("subs");
            if (!subscriber.isDisposed()) {
                while (iterator.hasNext()) {
                    subscriber.onNext(iterator.next());
                }
            }
            if (!subscriber.isDisposed()) {
                subscriber.onComplete();
            }
        });

        observable
                .subscribeOn(Schedulers.from(thread1)) //
                .observeOn(Schedulers.from(thread2)) //
                .map(x -> {
                    log("map");
                    return x + 1;
                }).subscribe(s -> log("observe " + s));

        observable
                .subscribeOn(Schedulers.from(thread1)) //
                .observeOn(Schedulers.from(thread3)) //
                .map(x -> {
                    log("map");
                    return x * 2;
                }).subscribe(s -> log("observe " + s));

        sseconds(3l);
    }

    @Test
    public void test1() {
        ObservableOnSubscribe<String> subscribeFunction = (s) -> {

            List<Future<String>> futures = IntStream.range(1, 5).mapToObj(i -> (Callable<String>) () -> {
                int sleepTime = random.nextInt(10);
                TimeUnit.SECONDS.sleep(sleepTime);
                return String.format("%d wait %d", i, sleepTime);
            }).map(ioPool::submit).collect(Collectors.toList());

            futures.forEach(f -> {
                if (!s.isDisposed()) {
                    try {
                        s.onNext(f.get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            });
            if (!s.isDisposed()) {
                s.onComplete();
            }
        };

        Observable.create(subscribeFunction)
                .subscribe((incomingValue) -> log("incomingValue " + incomingValue),
                        (error) -> log("Something went wrong" + error.getMessage()),
                        () -> log("This observable is finished"));
    }

    @Test
    public void primitiveProcessing() {
        Observable<Integer> just = Observable.just(1, 2, 3);

        List<Integer> single10 = just.map(i -> i + 10).toList().blockingGet();
        assertThat(single10, is(Arrays.asList(11, 12, 13)));

        List<Integer> single20 = just.map(i -> i + 20).toList().blockingGet();
        assertThat(single20, is(Arrays.asList(21, 22, 23)));

        List<Integer> collect = IntStream.range(0, 100).boxed().collect(Collectors.toList());

        Integer single = Observable.fromIterable(collect)
                .take(11)
                .filter(x -> x % 2 == 0)
                .map(x -> x * 2)
                .reduce(0, (x, y) -> x + y)
                .blockingGet();

        log(single);
    }

    @Test
    public void synchronousObservableExample() {
        Observable<Integer> observable = Observable.create(subscriber -> {
            PrimitiveIterator.OfInt iterator = IntStream.range(0, 3).iterator();
            if (!subscriber.isDisposed()) {
                while (iterator.hasNext()) {
                    subscriber.onNext(iterator.next());
                }
            }
            if (!subscriber.isDisposed()) {
                subscriber.onComplete();
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
                if (!subscriber.isDisposed()) {
                    while (iterator.hasNext()) {
                        subscriber.onNext(iterator.next());
                    }
                }
                if (!subscriber.isDisposed()) {
                    subscriber.onComplete();
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
                .subscribeOn(Schedulers.from(thread1))
                .observeOn(Schedulers.from(thread2))
                .doOnNext(x -> log(String.format("I  :%s:%d", Thread.currentThread().getName(), x)))

                .observeOn(Schedulers.from(thread3))
                .doOnNext(x -> log(String.format("II :%s:%d", Thread.currentThread().getName(), x)))
                .map(x -> x * 2)

                //.observeOn(Schedulers.from(thread3))
                .doOnNext(x -> log(String.format("III:%s:%d", Thread.currentThread().getName(), x)))
                .subscribe(x -> log(String.format("IV :%s:%d", Thread.currentThread().getName(), x)));

        sseconds(3l);

    }

    @Test
    public void intervalCold() {
        Observable<Long> interval = Observable.interval(1, TimeUnit.SECONDS, ioScheduler2);
        sseconds(3l);
        interval.observeOn(ioScheduler2).subscribe(this::log);
        sseconds(3l);
    }

    @Test
    public void intervalHot() {
        ConnectableObservable<Long> publish =
                Observable.interval(1, TimeUnit.SECONDS, ioScheduler2).publish();
        sseconds(2l);
        publish.observeOn(ioScheduler2).subscribe(this::log);
        publish.observeOn(ioScheduler2).subscribe(this::log);

        sseconds(3l);
    }

    @Test
    public void mergeConcatZip() {
        List<Integer> list1 = Arrays.asList(1, 2, 3, 4);
        List<Integer> list2 = Arrays.asList(5, 6, 7, 8, 9);
        Observable.fromIterable(Iterables.concat(list1, list2));
        Observable.fromIterable(list1).mergeWith(Observable.fromIterable(list2));
        Observable.zip(Observable.fromIterable(list1), Observable.fromIterable(list2), (x, y) -> x + y);
        Observable.fromIterable(list1).concatWith(Observable.fromIterable(list2));
        Observable.merge(Observable.fromIterable(list1), Observable.fromIterable(list2)).forEach(System.out::println);
    }


    Single<Integer> fromCallbackApi(int i) {
        return Single.create(emitter -> {
            callbackApi(i, (integer, throwable) -> {
                if (throwable == null) {
                    emitter.onSuccess(integer);
                } else {
                    emitter.onError(throwable);
                }
            });
        });
    }

    @Test
    public void callback2Rx() {
        Single<Integer> single5 = fromCallbackApi(5);
        Single<Integer> single6 = fromCallbackApi(6);
        Single.zip(single5, single6, (x, y) -> x + y).subscribe(this::log);
        sseconds(3l);
    }

}
