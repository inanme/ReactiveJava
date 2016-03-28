package org.inanme;

import org.inanme.HystrixModule.*;
import org.junit.Test;
import rx.Observable;

import java.util.concurrent.Future;

public class HystrixTest {

    @Test
    public void init() {
        String s = new Command1("Bob").execute();
        //Future<String> s = new CommandHelloWorld("Bob").queue();
        //Observable<String> s = new CommandHelloWorld("Bob").observe();

    }
}
