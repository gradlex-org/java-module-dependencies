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

import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSet;

import javax.inject.Inject;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public abstract class ModuleInfoCache {

    private final Map<File, ModuleInfo> moduleInfo = new HashMap<>();

    @Inject
    protected abstract ProviderFactory getProviders();

    @Inject
    protected abstract ProjectLayout getLayout();

    /**
     * Returns the module-info.java for the given SourceSet. If the SourceSet has multiple source folders with multiple
     * module-info files (which is usually a broken setup) the first file found is returned.
     *
     * @param sourceSet the SourceSet representing a module
     * @return parsed module-info.java for the given SourceSet
     */
    public ModuleInfo get(SourceSet sourceSet) {
        for (File folder : sourceSet.getJava().getSrcDirs()) {
            Provider<RegularFile> moduleInfoFile = getLayout().file(getProviders().provider(() -> new File(folder, "module-info.java")));
            Provider<String> moduleInfoContent = getProviders().fileContents(moduleInfoFile).getAsText();
            if (moduleInfoContent.isPresent()) {
                if (!moduleInfo.containsKey(folder)) {
                    moduleInfo.put(folder, new ModuleInfo(moduleInfoContent.get(), moduleInfoFile.get().getAsFile()));
                }
                return moduleInfo.get(folder);
            }
        }
        return ModuleInfo.EMPTY;
    }

    public boolean containsModule(String moduleName) {
        return moduleInfo.values().stream().anyMatch(x -> x.getModuleName().equals(moduleName));
    }
}
