package org.inanme;

import org.inanme.StockModule.Stock;
import org.inanme.StockModule.StockOption;
import org.inanme.StockModule.StockServer;
import org.inanme.StockModule.StockServerImpl;
import org.inanme.rxjava.Infra;
import org.junit.Test;
import rx.Observable;

public class StockServerTest extends Infra {

    @Test
    public void merge() {
        StockServer stockServer = new StockServerImpl(ioScheduler);
        Observable<StockOption> stock1 = stockServer.getFeed(new Stock("Stock1"));
        Observable<StockOption> stock2 = stockServer.getFeed(new Stock("Stock2"));
        Observable<StockOption> stock3 = stockServer.getFeed(new Stock("Stock3"));

        Observable.merge(stock1, stock2, stock3).reduce(0d, (sum, stock) -> sum + stock.value).toBlocking()
                  .forEach(System.out::println);
    }

    @Test
    public void zip() {
        StockServer stockServer = new StockServerImpl(ioScheduler);
        Observable<StockOption> stock1 = stockServer.getFeed(new Stock("Stock1"));
        Observable<StockOption> stock2 = stockServer.getFeed(new Stock("Stock2"));
        Observable<StockOption> stock3 = stockServer.getFeed(new Stock("Stock3"));

        Observable.zip(stock1, stock2, stock3, (p1, p2, p3) -> p1.value + p2.value + p3.value).toBlocking()
                  .forEach(System.out::println);
    }

    @Test
    public void concat() {
        StockServer stockServer = new StockServerImpl(ioScheduler);
        Observable<StockOption> stock1 = stockServer.getFeed(new Stock("Stock1"));
        Observable<StockOption> stock2 = stockServer.getFeed(new Stock("Stock2"));
        Observable<StockOption> stock3 = stockServer.getFeed(new Stock("Stock3"));

        Observable.concat(stock1, stock2, stock3).reduce(0d, (sum, stock) -> sum + stock.value).toBlocking()
                  .forEach(System.out::println);
    }
}
