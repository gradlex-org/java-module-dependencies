// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.test.fixture;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class WritableFile {

    private final Path file;

    public WritableFile(Path file) {
        this.file = file;
    }

    public WritableFile(Directory parent, String filePath) {
        this.file = Io.unchecked(() -> Files.createDirectories(
                        parent.getAsPath().resolve(filePath).getParent()))
                .resolve(Path.of(filePath).getFileName());
    }

    public WritableFile writeText(String text) {
        Io.unchecked(() -> Files.writeString(
                file, text, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
        return this;
    }

    public WritableFile appendText(String text) {
        Io.unchecked(() -> Files.writeString(file, text, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
        return this;
    }

    public WritableFile delete() {
        Io.unchecked(file, Files::delete);
        return this;
    }

    public boolean exists() {
        return Files.exists(file);
    }

    public Path getAsPath() {
        return file;
    }

    public String text() {
        return Io.unchecked(() -> Files.readString(file));
    }
}
