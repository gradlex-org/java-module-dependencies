/*
 * Copyright the GradleX team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradlex.javamodule.dependencies.test.fixture;

import java.io.IOException;
import java.io.UncheckedIOException;

class Io {

    private Io() {
    }

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
