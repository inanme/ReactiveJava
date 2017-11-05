package org.inanme;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import rx.Observable;

class HystrixModule {

    private HystrixModule() {
        throw new UnsupportedOperationException("dont");
    }

    static class Command1 extends HystrixCommand<String> {

        private final String name;

        Command1(String name) {
            super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"));
            this.name = name;
        }

        @Override
        protected String run() {
            return "Hello " + name + "!";
        }
    }

    static class Command2 extends HystrixObservableCommand<String> {

        private final String name;

        Command2(String name) {
            super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"));
            this.name = name;
        }

        @Override
        protected Observable<String> construct() {
            return Observable.just("Hello " + name + "!");
        }
    }

}
