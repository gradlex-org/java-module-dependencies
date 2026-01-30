// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradlex.javamodule.dependencies.internal.utils.ModuleInfo;
import org.jspecify.annotations.NullMarked;

@NullMarked
@CacheableTask
public abstract class MetaInfServicesGenerate extends DefaultTask {

    @Input
    @Optional
    public abstract Property<ModuleInfo> getModuleInfo();

    @OutputDirectory
    public abstract DirectoryProperty getDestinationDirectory();

    @TaskAction
    public void generateServiceProviderConfigurationFiles() throws IOException {
        Map<String, List<String>> serviceProvides = getModuleInfo().get().getProvides();

        Path services = getDestinationDirectory()
                .dir("META-INF/services")
                .get()
                .getAsFile()
                .toPath();

        Files.createDirectories(services);

        for (String service : serviceProvides.keySet()) {
            Path configurationFile = services.resolve(service);
            String serviceProvider = String.join("\n", serviceProvides.get(service));
            Files.write(configurationFile, serviceProvider.getBytes());
        }
    }
}
