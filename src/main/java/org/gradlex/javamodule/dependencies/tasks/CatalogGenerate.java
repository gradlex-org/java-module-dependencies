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

package org.gradlex.javamodule.dependencies.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class CatalogGenerate extends DefaultTask {

    public static class CatalogEntry implements Comparator<CatalogEntry> {
        private final String moduleName;
        private final String fullId;
        private final String version;

        public CatalogEntry(String moduleName, String fullId, @Nullable String version) {
            this.moduleName = moduleName;
            this.fullId = fullId;
            this.version = version;
        }

        @Override
        public int compare(CatalogEntry e1, CatalogEntry e2) {
            return e1.moduleName.compareTo(e2.moduleName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CatalogEntry that = (CatalogEntry) o;
            return Objects.equals(moduleName, that.moduleName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleName);
        }
    }

    @Internal
    public abstract SetProperty<CatalogEntry> getEntries();

    @Internal
    public abstract Property<String> getOwnProjectGroup();

    @Internal
    public abstract RegularFileProperty getCatalogFile();

    @TaskAction
    public void generate() throws IOException {
        File catalog = getCatalogFile().get().getAsFile();
        //noinspection ResultOfMethodCallIgnored
        catalog.getParentFile().mkdirs();

        List<String> content = new ArrayList<>();
        content.add("[libraries]");
        content.addAll(getEntries().get().stream().map(this::toDeclarationString).filter(Objects::nonNull).sorted().collect(Collectors.toList()));

        Files.write(catalog.toPath(), content);
    }

    @Nullable
    private String toDeclarationString(CatalogEntry entry) {
        String group = entry.fullId.split(":")[0];
        if (group.equals(getOwnProjectGroup().get())) {
            return null;
        }
        String notation;
        if (entry.version == null) {
            notation =  "{ module = \"" + entry.fullId + "\" }";
        } else {
            notation =  "{ module = \"" + entry.fullId + "\", version = \"" + entry.version + "\" }";
        }
        return entry.moduleName.replace('.', '-') + " = " + notation;
    }
}
