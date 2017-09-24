package org.inanme;

import com.google.common.base.MoreObjects;
import rx.Scheduler;
import rx.Single;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.TimeUnit;

class StockModule {

    private StockModule() {
        throw new UnsupportedOperationException("dont");
    }


    static class Stock {
        final String symbol;

        Stock(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("symbol", symbol).toString();
        }
    }

    static class StockOption {
        final Stock stock;

        final Double value;

        StockOption(Stock stock, Double value) {
            this.stock = stock;
            this.value = value;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("stock", stock).add("value", value).toString();
        }
    }

    interface StockServer {
        Single<StockOption> getFeed(Stock stock);
    }

    static class StockServerImpl implements StockServer {

        private final Random rng = new Random(System.currentTimeMillis());

        private final Scheduler scheduler;

        StockServerImpl(Scheduler scheduler) {
            this.scheduler = scheduler;
        }

        @Override
        public Single<StockOption> getFeed(Stock stock) {
            return Single.fromCallable(() -> privGetFeed(stock)).subscribeOn(scheduler);
            //return Single.just(privGetFeed(stock)).subscribeOn(scheduler);
            //return Single.defer(() -> Single.just(privGetFeed(stock))).subscribeOn(scheduler);
        }

        private StockOption privGetFeed(Stock stock) {
            Integer random = rng.nextInt(4);
            try {
                log("Will wait " + random);
                TimeUnit.SECONDS.sleep(random.longValue());
                return new StockOption(stock, random.doubleValue());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        void log(Object log) {
            String now = LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_TIME);
            System.err.format("%s %s: %s %n", now, Thread.currentThread().getName(), log);
        }
    }
}
