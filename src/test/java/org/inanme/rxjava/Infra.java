package org.inanme.rxjava;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import rx.schedulers.Schedulers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class Infra {
    final ExecutorService ioPool = Executors.newFixedThreadPool(4);

    final private Timer timer = new Timer();

    final protected rx.Scheduler ioScheduler = Schedulers.from(ioPool);

    final io.reactivex.Scheduler ioScheduler2 = io.reactivex.schedulers.Schedulers.from(ioPool);

    final private AtomicInteger threadFactory0 = new AtomicInteger();

    final private AtomicInteger threadFactory1 = new AtomicInteger();

    final private AtomicInteger threadFactory2 = new AtomicInteger();

    final private AtomicInteger threadFactory3 = new AtomicInteger();

    ExecutorService thread0 =
            Executors.newCachedThreadPool(r -> new Thread(r, "thread0-" + threadFactory0.getAndIncrement()));

    ExecutorService thread1 =
            Executors.newCachedThreadPool(r -> new Thread(r, "thread1-" + threadFactory1.getAndIncrement()));

    ExecutorService thread2 =
            Executors.newCachedThreadPool(r -> new Thread(r, "thread2-" + threadFactory2.getAndIncrement()));

    ExecutorService thread3 =
            Executors.newCachedThreadPool(r -> new Thread(r, "thread3-" + threadFactory3.getAndIncrement()));

    Random random = new Random(System.currentTimeMillis());

    List<Integer> _123 = Arrays.asList(1, 2, 3);

    private long start;

    class Waiter implements Callable<Long> {

        private final long seconds;

        Waiter(long seconds) {
            this.seconds = seconds;
        }

        @Override
        public Long call() {
            try {
                log("will sleep " + seconds);
                TimeUnit.SECONDS.sleep(seconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return seconds;
        }
    }

    @BeforeEach
    public void start(TestInfo testInfo) {
        System.err.printf("Test %s started: %s\n", testInfo.getDisplayName(), now());
        start = System.currentTimeMillis();
    }

    private String now() {
        return LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_TIME);
    }

    @AfterEach
    public void finish(TestInfo testInfo) {
        System.err.printf("Test %s ended  : %s%n", testInfo.getDisplayName(), now());
        long duration = System.currentTimeMillis() - start;
        System.err.printf("Test %s took   : %s%n", testInfo.getDisplayName(),
                DurationFormatUtils.formatDurationWords(duration, false, false));
    }

     void sseconds(long seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    void smilis(long milis) {
        try {
            TimeUnit.MILLISECONDS.sleep(milis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    Future<Long> future(long seconds) {
        return ioPool.submit(new Waiter(seconds));
    }

    void callbackApi(int input, BiConsumer<Integer, Throwable> cb) {
        Objects.requireNonNull(cb);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                cb.accept(input, null);
            }
        }, input * 100L);
    }

    protected void log(Object log) {
        System.err.format("%s %s: %s %n", now(), Thread.currentThread().getName(), log);
    }
}
