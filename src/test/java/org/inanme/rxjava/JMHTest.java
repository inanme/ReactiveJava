package org.inanme.rxjava;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
public class JMHTest {

    @Test
    public void test() throws IOException, RunnerException {
        String[] args = {};
        Main.main(args);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 5)
    @Measurement(iterations = 5)
    @Fork(3)
    public void test000() {
        ContiguousSet<Integer> integers = ContiguousSet.create(Range.closed(1, 1000), DiscreteDomain.integers());
        integers.stream().mapToInt(Integer::intValue).sum();
    }
}
