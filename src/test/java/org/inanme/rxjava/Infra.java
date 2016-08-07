package org.inanme.rxjava;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Infra {

    @Rule
    public TestName name = new TestName();

    protected ExecutorService ioPool = Executors.newFixedThreadPool(4);

    protected Scheduler ioScheduler = Schedulers.from(ioPool);

    private AtomicInteger threadFactory1 = new AtomicInteger();

    private AtomicInteger threadFactory2 = new AtomicInteger();

    private AtomicInteger threadFactory3 = new AtomicInteger();

    protected ExecutorService thread1 =
        Executors.newCachedThreadPool(r -> new Thread(r, "thread1-" + threadFactory1.getAndIncrement()));

    protected ExecutorService thread2 =
        Executors.newCachedThreadPool(r -> new Thread(r, "thread2-" + threadFactory2.getAndIncrement()));

    protected ExecutorService thread3 =
        Executors.newCachedThreadPool(r -> new Thread(r, "thread3-" + threadFactory3.getAndIncrement()));

    protected Random random = new Random(System.currentTimeMillis());

    private long start;

    protected class Waiter implements Callable<Long> {

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

    @Before
    public void start() {
        System.out.printf("Test %s started: %s\n", name.getMethodName(), now());
        start = System.currentTimeMillis();
    }

    String now() {
        return LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_DATE_TIME);
    }

    @After
    public void finish() {
        System.out.printf("Test %s ended  : %s\n", name.getMethodName(), now());
        long duration = System.currentTimeMillis() - start;
        System.out.printf("Test %s took   : %s\n", name.getMethodName(),
                          DurationFormatUtils.formatDurationWords(duration, false, false));
    }

    protected void giveMeTime(long seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void log(Object log) {
        System.out.format("%s %s: %s \n", now(), Thread.currentThread().getName(), log);
    }
}
