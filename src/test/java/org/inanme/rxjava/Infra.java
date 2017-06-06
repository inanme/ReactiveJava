package org.inanme.rxjava;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import rx.schedulers.Schedulers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class Infra {

    @Rule
    public TestName name = new TestName();

    protected ExecutorService ioPool = Executors.newFixedThreadPool(4);

    protected Timer timer = new Timer();

    protected rx.Scheduler ioScheduler = Schedulers.from(ioPool);

    protected io.reactivex.Scheduler ioScheduler2 = io.reactivex.schedulers.Schedulers.from(ioPool);

    private AtomicInteger threadFactory0 = new AtomicInteger();

    private AtomicInteger threadFactory1 = new AtomicInteger();

    private AtomicInteger threadFactory2 = new AtomicInteger();

    private AtomicInteger threadFactory3 = new AtomicInteger();

    protected ExecutorService thread0 =
            Executors.newCachedThreadPool(r -> new Thread(r, "thread0-" + threadFactory0.getAndIncrement()));

    protected ExecutorService thread1 =
            Executors.newCachedThreadPool(r -> new Thread(r, "thread1-" + threadFactory1.getAndIncrement()));

    protected ExecutorService thread2 =
            Executors.newCachedThreadPool(r -> new Thread(r, "thread2-" + threadFactory2.getAndIncrement()));

    protected ExecutorService thread3 =
            Executors.newCachedThreadPool(r -> new Thread(r, "thread3-" + threadFactory3.getAndIncrement()));

    protected Random random = new Random(System.currentTimeMillis());

    protected List<Integer> _123 = Arrays.asList(1, 2, 3);

    private long start;

    protected class Waiter implements Callable<Long> {

        private final long seconds;

        public Waiter(long seconds) {
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

    @Before
    public void start() {
        System.err.printf("Test %s started: %s\n", name.getMethodName(), now());
        start = System.currentTimeMillis();
    }

    String now() {
        return LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_TIME);
    }

    @After
    public void finish() {
        System.err.printf("Test %s ended  : %s\n", name.getMethodName(), now());
        long duration = System.currentTimeMillis() - start;
        System.err.printf("Test %s took   : %s\n", name.getMethodName(),
                DurationFormatUtils.formatDurationWords(duration, false, false));
    }

    protected void sseconds(long seconds) {
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

    public void log(Object log) {
        System.err.format("%s %s: %s \n", now(), Thread.currentThread().getName(), log);
    }
}
