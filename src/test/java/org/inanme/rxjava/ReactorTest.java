package org.inanme.rxjava;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

class ReactorTest {

    private static List<String> words = Arrays.asList("the", "quick", "brown", "fox", "jumped",
            "over", "the", "lazy", "dog");

    @Test
    void simpleCreation() {
        Flux<String> fewWords = Flux.just("Hello", "World");
        Flux<String> manyWords = Flux.fromIterable(words);

        fewWords.subscribe(System.out::println);
        manyWords.subscribe(System.out::println);
    }

    @Test
    void findingMissingLetter() {
        Flux<String> manyLetters = Flux
                .fromIterable(words)
                .flatMap(word -> Flux.fromArray(word.split("")))
                .distinct()
                .sort()
                .zipWith(Flux.range(1, Integer.MAX_VALUE),
                        (string, count) -> String.format("%2d. %s", count, string));

        manyLetters.subscribe(System.out::println);
    }

    @Test
    void blocks() {
        Flux<String> helloPauseWorld = Mono.just("Hello").concatWith(Mono.just("world"));
        helloPauseWorld.toStream().forEach(System.out::println);
    }

}
