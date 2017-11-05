package org.inanme;

import org.inanme.StockModule.Stock;
import org.inanme.StockModule.StockOption;
import org.inanme.StockModule.StockServer;
import org.inanme.StockModule.StockServerImpl;
import org.inanme.rxjava.Infra;
import org.junit.jupiter.api.Test;
import rx.Single;

class StockServerTest extends Infra {

    @Test
    void merge() {
        StockServer stockServer = new StockServerImpl(ioScheduler);
        Single<StockOption> stock1 = stockServer.getFeed(new Stock("Stock1"));
        Single<StockOption> stock2 = stockServer.getFeed(new Stock("Stock2"));
        Single<StockOption> stock3 = stockServer.getFeed(new Stock("Stock3"));

        Single.merge(stock1, stock2, stock3)
                .reduce(0d, (sum, stock) -> sum + stock.value)
                .toBlocking()
                .forEach(this::log);
    }

    @Test
    void zip() {
        StockServer stockServer = new StockServerImpl(ioScheduler);
        Single<StockOption> stock1 = stockServer.getFeed(new Stock("Stock1"));
        Single<StockOption> stock2 = stockServer.getFeed(new Stock("Stock2"));
        Single<StockOption> stock3 = stockServer.getFeed(new Stock("Stock3"));

        double value = Single.zip(stock1, stock2, stock3, (p1, p2, p3) -> p1.value + p2.value + p3.value)
                .toBlocking()
                .value();
        log(value);
    }

    @Test
    void concat() {
        StockServer stockServer = new StockServerImpl(ioScheduler);
        Single<StockOption> stock1 = stockServer.getFeed(new Stock("Stock1"));
        Single<StockOption> stock2 = stockServer.getFeed(new Stock("Stock2"));
        Single<StockOption> stock3 = stockServer.getFeed(new Stock("Stock3"));

        Single.concat(stock1, stock2, stock3)
                .reduce(0d, (sum, stock) -> sum + stock.value)
                .toBlocking()
                .forEach(this::log);
    }
}
