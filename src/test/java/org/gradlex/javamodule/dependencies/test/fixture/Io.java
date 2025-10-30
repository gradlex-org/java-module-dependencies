// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.test.fixture;

import java.io.IOException;
import java.io.UncheckedIOException;

class Io {

    private Io() {}

    static <T> T unchecked(IoSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static <T> void unchecked(T t, IoConsumer<T> consumer) {
        try {
            consumer.accept(t);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @FunctionalInterface
    interface IoSupplier<T> {
        T get() throws IOException;
    }

    @FunctionalInterface
    interface IoConsumer<T> {
        void accept(T t) throws IOException;
    }
}
