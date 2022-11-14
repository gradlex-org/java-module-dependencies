package org.gradlex.javamodule.dependencies.internal.utils;

import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;

public class TaskConfigurationUtil {

    public static boolean isJavaCompileTask(Task task, SourceSet sourceSet) {
        return task instanceof JavaCompile && task.getName().equals(sourceSet.getCompileJavaTaskName());
    }

    public static boolean isJavadocTask(Task task, SourceSet sourceSet) {
        return task instanceof Javadoc && task.getName().equals(sourceSet.getJavadocTaskName());
    }
}
