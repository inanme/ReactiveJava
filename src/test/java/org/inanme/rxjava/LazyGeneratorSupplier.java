package org.inanme.rxjava;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

public class LazyGeneratorSupplier {

    static <T> com.google.common.base.Supplier<T> supplier(T seed, UnaryOperator<T> f) {
        WeakReference<T> weakref = new WeakReference<>(checkNotNull(seed));
        checkNotNull(f);
        return new com.google.common.base.Supplier<T>() {
            private T next;
            private boolean first = true;

            private int counter;

            @Override
            public T get() {
                if (first) {
                    first = false;
                    return weakref.get();
                } else {
                    if (next == null) {
                        next = f.apply(weakref.get());
                        weakref.clear();
                    } else {
                        try {
                            T temp = f.apply(next);
                            Preconditions.checkArgument(counter++ != 3);
                            next = temp;
                            System.err.println("------------");
                        } catch (Exception e) {
                            System.err.println("------------Failed");
                        }
                    }
                    return next;
                }
            }
        };
    }

    @Test
    public void testMapJoiner() throws InterruptedException {
        Supplier<Long> integerSupplier = Suppliers.memoizeWithExpiration(supplier(0L, Math::incrementExact), 1L, TimeUnit.SECONDS);
        rx.Observable.interval(500L, TimeUnit.MILLISECONDS).forEach(lvalue ->
                System.err.println(format("%d %d", lvalue, integerSupplier.get()))
        );

        TimeUnit.SECONDS.sleep(10l);
    }

}
