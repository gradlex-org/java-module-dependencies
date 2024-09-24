package org.gradlex.javamodule.dependencies.internal.utils;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public abstract class ValueSourceModuleInfo implements ValueSource<ModuleInfo, ValueSourceModuleInfo.ModuleInfoSourceP> {


    interface ModuleInfoSourceP extends ValueSourceParameters {

        DirectoryProperty getDir();
    }


    @Override
    public @Nullable ModuleInfo obtain() {
        ModuleInfoSourceP parameters = getParameters();
        File file = new File(parameters.getDir().get().getAsFile(), "module-info.java");
        if (file.isFile()) {
            try {
                Scanner scan = new Scanner(file);
                try {
                    scan.useDelimiter("\\Z");
                    String content = scan.next();
                    return new ModuleInfo(content);
                } finally {
                    scan.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
