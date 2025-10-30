// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.test.fixture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class Directory {

    private final Path directory;

    Directory(Path directory) {
        this.directory = directory;
        Io.unchecked(() -> Files.createDirectories(directory));
    }

    public WritableFile file(String path) {
        Path file = directory.resolve(path);
        Io.unchecked(() -> Files.createDirectories(file.getParent()));
        return new WritableFile(file);
    }

    public Directory dir(String path) {
        Path dir = directory.resolve(path);
        return new Directory(dir);
    }

    public void delete() {
        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> Io.unchecked(p, Files::delete));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Path getAsPath() {
        return directory;
    }

    public String canonicalPath() {
        return Io.unchecked(() -> getAsPath().toFile().getCanonicalPath());
    }
}
