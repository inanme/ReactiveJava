package org.inanme;

import com.google.common.base.MoreObjects;
import rx.Observable;
import rx.Scheduler;

import java.util.Random;
import java.util.concurrent.TimeUnit;

class StockModule {

    static class Stock {
        final String symbol;

        Stock(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("symbol", symbol)
                    .toString();
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
            return MoreObjects.toStringHelper(this)
                    .add("stock", stock)
                    .add("value", value)
                    .toString();
        }
    }

    interface StockServer {
        Observable<StockOption> getFeed(Stock stock);
    }

    static class StockServerImpl implements StockServer {

        private final Random rng = new Random(System.currentTimeMillis());

        private final Scheduler scheduler;

        StockServerImpl(Scheduler scheduler) {
            this.scheduler = scheduler;
        }

        @Override
        public Observable<StockOption> getFeed(Stock stock) {
            return Observable.<StockOption>create(subscriber -> {
                Integer random = rng.nextInt(4);
                try {
                    TimeUnit.SECONDS.sleep(random.longValue());
                } catch (InterruptedException e) {
                    subscriber.onError(e);
                }
                subscriber.onNext(new StockOption(stock, random.doubleValue()));
                subscriber.onCompleted();
            }).subscribeOn(scheduler);
        }
    }
}
