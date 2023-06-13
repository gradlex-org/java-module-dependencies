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

import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;

public abstract class TaskConfigurationUtil {

    public static boolean isJavaCompileTask(Task task, SourceSet sourceSet) {
        return task instanceof JavaCompile && task.getName().equals(sourceSet.getCompileJavaTaskName());
    }

    public static boolean isJavadocTask(Task task, SourceSet sourceSet) {
        return task instanceof Javadoc && task.getName().equals(sourceSet.getJavadocTaskName());
    }
}
