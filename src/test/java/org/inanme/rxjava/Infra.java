package org.inanme.rxjava;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Infra {

    @Rule
    public TestName name = new TestName();

    protected SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    protected ExecutorService service = Executors.newFixedThreadPool(3);

    protected ExecutorService thread1 = Executors.newCachedThreadPool(r -> new Thread(r, "thread1"));

    protected ExecutorService thread2 = Executors.newCachedThreadPool(r -> new Thread(r, "thread2"));

    protected ExecutorService thread3 = Executors.newCachedThreadPool(r -> new Thread(r, "thread3"));

    protected Random random = new Random();

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
        System.out.printf("Test %s started: %s\n", name.getMethodName(), sdf.format(new Date()));
        start = System.currentTimeMillis();
    }

    @After
    public void finish() {
        System.out.printf("Test %s ended  : %s\n", name.getMethodName(), sdf.format(new Date()));
        long duration = System.currentTimeMillis() - start;
        System.out.printf("Test %s took   : %s\n", name.getMethodName(), DurationFormatUtils.formatDurationWords(duration, false, false));
    }

    protected void giveMeTime(long seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void log(Object log) {
        System.out.println(Thread.currentThread().getName() + " " + log);
    }

}
