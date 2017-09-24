package org.inanme;

import org.inanme.HystrixModule.Command1;
import org.junit.jupiter.api.Test;


class HystrixTest {

    @Test
    void init() {
        String s = new Command1("Bob").execute();
        //Future<String> s = new CommandHelloWorld("Bob").queue();
        //Observable<String> s = new CommandHelloWorld("Bob").observe();

    }
}
