package org.gradlex.javamodule.dependencies.internal.utils;

import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.*;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Scanner;

public abstract class ValueSourceModuleInfo implements ValueSource<ModuleInfo, ValueSourceModuleInfo.ModuleInfoSourceP> {


    interface ModuleInfoSourceP extends ValueSourceParameters {

        Property<FileCollection> getLocations();
    }


    @Override
    public @Nullable ModuleInfo obtain() {
        ModuleInfoSourceP parameters = getParameters();
        for (File fileSystemLocation : parameters.getLocations().get()) {
            File file = new File(fileSystemLocation, "module-info.java");
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


        }
        return null;
    }
}
