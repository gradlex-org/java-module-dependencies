// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.internal.utils;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.jetbrains.annotations.Nullable;

public abstract class ValueSourceModuleInfo implements ValueSource<ModuleInfo, ValueSourceModuleInfo.Parameter> {

    interface Parameter extends ValueSourceParameters {
        DirectoryProperty getDir();
    }

    @Override
    public @Nullable ModuleInfo obtain() {
        Parameter parameters = getParameters();
        File file = new File(parameters.getDir().get().getAsFile(), "module-info.java");
        if (file.isFile()) {
            try {
                try (Scanner scan = new Scanner(file)) {
                    scan.useDelimiter("\\Z");
                    String content = scan.next();
                    return new ModuleInfo(content);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
