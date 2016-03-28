package org.inanme;

import org.inanme.StockModule.*;
import org.inanme.rxjava.Infra;
import org.junit.Test;
import rx.Observable;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class StockServerTest extends Infra {

    @Test
    public void someTest() {
        StockServer stockServer = new StockServerImpl(ioScheduler);
        Observable<StockOption> stock1 = stockServer.getFeed(new Stock("Stock1"));
        Observable<StockOption> stock2 = stockServer.getFeed(new Stock("Stock2"));
        Observable<StockOption> stock3 = stockServer.getFeed(new Stock("Stock3"));

        Observable.concat(stock1, stock2, stock3)
                .reduce(0d, (sum, stock) -> sum + stock.value)
                .forEach(System.out::println);

//        Observable.zip(stock1, stock2, stock3, (p1, p2, p3) -> p1.value + p2.value + p3.value)
//                .forEach(System.out::println);

        giveMeTime(7l);
    }

}