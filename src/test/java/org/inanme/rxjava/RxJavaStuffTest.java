package org.inanme.rxjava;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;
import rx.Observable;
import rx.Single;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;
import rx.subjects.AsyncSubject;
import rx.subjects.BehaviorSubject;
import rx.subjects.ReplaySubject;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class RxJavaStuffTest extends Infra {

    @Test
    void zip() {
        Observable<Long> task1 = Observable.from(future(2));
        Observable<Long> task2 = Observable.from(future(3));
        Observable<Long> task3 = Observable.from(future(4));
        Observable<Long> zip = Observable.zip(task1, task2, task3, (x, y, z) -> x + y + z);
        zip.subscribe(this::log);
    }

    @Test
    void concat() {
        Observable<Long> task1 = Observable.from(future(2));
        Observable<Long> task2 = Observable.from(future(3));
        Observable<Long> task3 = Observable.from(future(4));
        Observable.concat(task1, task2, task3).forEach(this::log);
    }

    @Test
    void merge() {
        Observable<Long> task1 = Observable.from(future(2));
        Observable<Long> task2 = Observable.from(future(3));
        Observable<Long> task3 = Observable.from(future(4));
        Observable.merge(task1, task2, task3).forEach(this::log);
    }

    @Test
    void test0() {
        Observable<Integer> observable = Observable.unsafeCreate(subscriber -> {
            Iterator<Integer> iterator = _123.iterator();
            log("subs");
            while (iterator.hasNext()) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(iterator.next());
                }
            }
            if (!subscriber.isUnsubscribed()) {
                subscriber.onCompleted();
            }
        });

        observable
                .subscribeOn(Schedulers.from(thread1)) //
                .observeOn(Schedulers.from(thread2)) //
                .map(x -> {
                    log("map+1");
                    return x + 1;
                }).subscribe(s -> log("observe+1 " + s));

        observable
                .subscribeOn(Schedulers.from(thread1)) //
                .observeOn(Schedulers.from(thread3)) //
                .map(x -> {
                    log("map*2");
                    return x * 2;
                }).subscribe(s -> log("observe*2 " + s));

        sseconds(3l);
    }

    @Test
    void test1() {
        Observable.OnSubscribe<String> subscribeFunction = (s) -> {

            List<Future<String>> futures = IntStream.range(1, 5)
                    .mapToObj(i -> (Callable<String>) () -> {
                        int sleepTime = random.nextInt(10);
                        TimeUnit.SECONDS.sleep(sleepTime);
                        return String.format("%d wait %d", i, sleepTime);
                    }).map(ioPool::submit)
                    .collect(Collectors.toList());

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

        Observable.unsafeCreate(subscribeFunction)
                .subscribe((incomingValue) -> log("incomingValue " + incomingValue),
                        (error) -> log("Something went wrong" + error.getMessage()),
                        () -> log("This observable is finished"));
    }

    @Test
    void primitiveProcessing() {
        Observable<Integer> just = Observable.just(1, 2, 3);

        List<Integer> single10 = just.map(i -> i + 10).toList().toBlocking().single();
        assertThat(single10, is(Arrays.asList(11, 12, 13)));

        List<Integer> single20 = just.map(i -> i + 20).toList().toBlocking().single();
        assertThat(single20, is(Arrays.asList(21, 22, 23)));

        List<Integer> collect = IntStream.range(0, 100).boxed().collect(Collectors.toList());

        Integer single = Observable.from(collect)
                .take(11)
                .filter(x -> x % 2 == 0)
                .map(x -> x * 2)
                .reduce(0, (x, y) -> x + y)
                .toBlocking().single();

        log(single);
    }

    @Test
    void synchronousObservableExample() {
        Observable<Integer> observable = Observable.unsafeCreate(subscriber -> {
            Iterator<Integer> iterator = _123.iterator();
            while (iterator.hasNext()) {
                if (!subscriber.isUnsubscribed()) {
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
    void asynchronousObservableExample() throws InterruptedException {
        Observable<Integer> observable = Observable.unsafeCreate(subscriber -> {
            ioPool.submit(() -> {
                Iterator<Integer> iterator = _123.iterator();
                while (iterator.hasNext()) {
                    if (!subscriber.isUnsubscribed()) {
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
    void none() {

        Observable.just(1, 2, 3)
                //Asynchronously subscribes Observers to this Observable
                .subscribeOn(Schedulers.from(thread0))
                .observeOn(Schedulers.from(thread1))
                .doOnNext(x -> log("I  :" + x))

                .observeOn(Schedulers.from(thread2))
                .doOnNext(x -> log("II :" + x))
                .map(x -> x * 2)

                //.observeOn(Schedulers.from(thread3))
                .doOnNext(x -> log("III:" + x))
                .subscribe(x -> log("IV :" + x));

        sseconds(3l);

    }

    @Test
    void intervalCold() {
        Observable<Long> interval = Observable.interval(1, TimeUnit.SECONDS, ioScheduler);
        sseconds(3l);
        interval.observeOn(ioScheduler).subscribe(this::log);
        sseconds(3l);
    }

    @Test
    void backpressure() {
        Observable.interval(10, TimeUnit.MILLISECONDS, Schedulers.from(thread0))
                .onBackpressureLatest()
                //.onBackpressureBuffer()
                //.onBackpressureDrop(l -> log("Drop " + l))
                .observeOn(Schedulers.from(thread1))
                .doOnNext(l -> smilis(300))
                .subscribe(this::log);

        sseconds(3l);
    }

    @Test
    void backpressure1() {
        Observable.<Long>unsafeCreate(subscriber -> {
            LongStream.range(1, 100).forEach(l -> {
                        subscriber.onNext(l);
                        smilis(4);
                    }
            );
            subscriber.onCompleted();
        })
                .onBackpressureBuffer(10, () -> log("overflow"))
                //.onBackpressureDrop(l -> log("Drop " + l))
                .observeOn(Schedulers.from(thread1))
                .doOnNext(l -> smilis(400))
                .subscribe(this::log);

        sseconds(10000l);
    }

    @Test
    void intervalHot() {
        ConnectableObservable<Long> publish =
                Observable.interval(1, TimeUnit.SECONDS, ioScheduler).publish();
        sseconds(3l);
        publish.observeOn(ioScheduler).subscribe(this::log);
        publish.observeOn(ioScheduler).subscribe(this::log);

        sseconds(3l);
    }

    @Test
    void mergeConcatZip() {
        List<Integer> list1 = Arrays.asList(1, 2, 3, 4);
        List<Integer> list2 = Arrays.asList(5, 6, 7, 8, 9);
        Observable.from(Iterables.concat(list1, list2));
        Observable.from(list1).mergeWith(Observable.from(list2));
        Observable.zip(Observable.from(list1), Observable.from(list2), (x, y) -> x + y);
        Observable.from(list1).concatWith(Observable.from(list2));
        Observable.merge(Observable.from(list1), Observable.from(list2)).forEach(this::log);
    }

    @Test
    void repeatWhen() {
        Observable.from(_123).repeat(2).subscribe(this::log);
    }

    @Test
    void asyncSubject() {
        AsyncSubject<Integer> subject = AsyncSubject.create();
        subject.subscribe(this::log);
        _123.forEach(subject::onNext);
        subject.onCompleted();
    }

    @Test
    void behaviorSubject() {
        BehaviorSubject<Integer> subject = BehaviorSubject.create(0);
        _123.forEach(subject::onNext);
        subject.subscribe(this::log);
        _123.forEach(subject::onNext);
        subject.onCompleted();
    }

    @Test
    void replaySubject() {
        ReplaySubject<Integer> subject = ReplaySubject.create();
        subject.subscribe(this::log);
        subject.subscribe(this::log);
        _123.forEach(subject::onNext);
        subject.onCompleted();
    }

    Single<Integer> fromCallbackApi(int i) {
        return Single.fromEmitter(emitter -> {
            callbackApi(i, (integer, throwable) -> {
                if (throwable == null) {
                    emitter.onSuccess(integer);
                } else {
                    emitter.onError(throwable);
                }
            });
        });
    }

}
