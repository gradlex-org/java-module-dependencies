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

package org.gradlex.javamodule.dependencies.internal.utils;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public abstract class ModuleInfoValueSource implements ValueSource<ModuleInfo, ModuleInfoValueSource.ModuleInfoValueSourceParameter> {

    interface ModuleInfoValueSourceParameter extends ValueSourceParameters {
        DirectoryProperty getDir();
    }

    @Override
    public @Nullable ModuleInfo obtain() {
        ModuleInfoValueSourceParameter parameters = getParameters();
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
