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

package org.gradlex.javamodule.dependencies.initialization;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.util.GradleVersion;

public abstract class JavaModuleDependenciesSettingsPlugin implements Plugin<Settings> {

    @Override
    public void apply(Settings settings) {
        if (GradleVersion.current().compareTo(GradleVersion.version("8.8")) < 0) {
            throw new GradleException("This settings plugin requires Gradle 8.8+");
        }
        registerExtension(settings);
    }

    private void registerExtension(Settings settings) {
        settings.getExtensions().create("rootPlugins", RootPluginsExtension.class, settings);
        settings.getExtensions().create("javaModules", JavaModulesExtension.class, settings);
    }
}
