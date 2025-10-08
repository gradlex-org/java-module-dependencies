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

package org.gradlex.javamodule.dependencies;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class LocalModule implements Comparable<LocalModule>, Serializable {
    private final String moduleName;
    private final String projectPath;
    @Nullable
    private final String capability;

    public LocalModule(String moduleName, String projectPath, @Nullable String capability) {
        this.moduleName = moduleName;
        this.projectPath = projectPath;
        this.capability = capability;
    }

    /**
     * @return the module name as defined in the module-info.java file, e.g. 'org.example.module-a'
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * @return the full Gradle project path for the subproject defining the module, e.g. ':module-a'
     */
    public String getProjectPath() {
        return projectPath;
    }

    /**
     * @return the capability to address the module in a dependency if it is not in the 'main' source set, e.g. 'org.example:module-a-test-fixtures'
     */
    @Nullable
    public String getCapability() {
        return capability;
    }

    @Override
    public String toString() {
        return "[" +
                "moduleName='" + moduleName + '\'' +
                ", projectPath='" + projectPath + '\'' +
                ", capability='" + capability + '\'' +
                ']';
    }

    @Override
    public int compareTo(LocalModule o) {
        return moduleName.compareTo(o.moduleName);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LocalModule that = (LocalModule) o;
        return Objects.equals(moduleName, that.moduleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleName);
    }
}
