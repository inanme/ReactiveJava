package org.inanme.rxjava;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import rx.Observable;

import java.util.*;
import java.util.concurrent.TimeUnit;

import rx.subjects.*;

public class RxPaging {

    public static class Pagination1 extends Infra{

        @Test
        public void test1() throws InterruptedException {
            Map<String, List<String>> pages = ImmutableMap.<String, List<String>>builder()
                    .put("1", Arrays.asList("2", "11", "12", "13"))
                    .put("2", Arrays.asList("3", "21", "22", "23"))
                    .put("3", Arrays.asList("4", "31", "32", "33"))
                    .put("4", Arrays.asList("", "41", "42", "43"))
                    .build();

            Observable<List<String>> ret = Observable.defer(() -> {
                ReplaySubject<String> pagecontrol = ReplaySubject.create(1);
                pagecontrol.onNext("1");
                Observable<List<String>> ret2 = pagecontrol.asObservable().concatMap(aKey -> {
                    if (!aKey.equals("")) {
                        return Observable.just(pages.get(aKey)).doOnNext(page -> pagecontrol.onNext(page.get(0)));
                    } else {
                        return Observable.<List<String>>empty().doOnCompleted(() -> pagecontrol.onCompleted());
                    }
                });
                return ret2;
            });
            // Use this if you want to ensure work isn't done again
            ret = ret.cache();
            ret.toBlocking().subscribe(l -> log("Sub 1 : " + l));
            ret.toBlocking().subscribe(l -> log("Sub 2 : " + l));
        }
    }

    public static class Pagination2 {

        Observable<Window> paging() {

            Subject<Token, Token> tokenStream = BehaviorSubject.create();

            tokenStream.onNext(Token.startToken());

            Observable<Window> dataStream =
                    Observable.defer(() -> tokenStream.first().flatMap(this::remoteData))
                            .doOnNext(window -> tokenStream.onNext(window.getToken()))
                            .repeatWhen(completed -> completed.flatMap(__ -> tokenStream).takeWhile(Token::hasMore));

            return dataStream;
        }

        private Observable<Window> remoteData(Token token) {
        /*limit number of pages*/
            int page = page(token);
            Token nextToken = page < 10
                    ? nextPageToken(token)
                    : Token.endToken();

            return Observable
                    .just(new Window(nextToken, "data for token: " + token))
                    .delay(100, TimeUnit.MILLISECONDS);
        }

        private int page(Token token) {
            String key = token.getKey();
            return key.isEmpty() ? 0 : Integer.parseInt(key);
        }

        private Token nextPageToken(Token token) {
            String tokenKey = token.getKey();
            return tokenKey.isEmpty() ? new Token("1") : nextToken(tokenKey);
        }

        private Token nextToken(String tokenKey) {
            return new Token(String.valueOf(Integer.parseInt(tokenKey) + 1));
        }

        public static class Token {
            private final String key;

            private Token(String key) {
                this.key = key;
            }

            public static Token endToken() {
                return startToken();
            }

            public static Token startToken() {
                return new Token("");
            }

            public String getKey() {
                return key;
            }

            public boolean hasMore() {
                return !key.isEmpty();
            }

            @Override
            public String toString() {
                return "Token{" +
                        "key='" + key + '\'' +
                        '}';
            }
        }


        public static class Window {
            private final Token token;
            private final String data;

            public Window(Token token, String data) {
                this.token = token;
                this.data = data;
            }

            public Token getToken() {
                return token;
            }

            public String getData() {
                return data;
            }

            @Override
            public String toString() {
                return "Window{" +
                        "next token=" + token +
                        ", data='" + data + '\'' +
                        '}';
            }
        }

        @Test
        public void testPaging() throws Exception {
            paging().toBlocking().subscribe(System.out::println);
        }
    }

}