/*
 * Copyright 2022 the GradleX team.
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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

public class ModuleJar {
    private static final String AUTOMATIC_MODULE_NAME_ATTRIBUTE = "Automatic-Module-Name";
    private static final String MULTI_RELEASE_ATTRIBUTE = "Multi-Release";
    private static final String MODULE_INFO_CLASS_FILE = "module-info.class";
    private static final Pattern MODULE_INFO_CLASS_MRJAR_PATH = Pattern.compile("META-INF/versions/\\d+/module-info.class");

    public static String readNameFromModuleFromJarFile(File jarFile) throws IOException {
        try (JarInputStream jarStream =  new JarInputStream(Files.newInputStream(jarFile.toPath()))) {
            String moduleName = getAutomaticModuleName(jarStream.getManifest());
            if (moduleName != null) {
                return moduleName;
            }
            boolean isMultiReleaseJar = containsMultiReleaseJarEntry(jarStream);
            ZipEntry next = jarStream.getNextEntry();
            while (next != null) {
                if (MODULE_INFO_CLASS_FILE.equals(next.getName())) {
                    return readNameFromModuleInfoClass(jarStream);
                }
                if (isMultiReleaseJar && MODULE_INFO_CLASS_MRJAR_PATH.matcher(next.getName()).matches()) {
                    return readNameFromModuleInfoClass(jarStream);
                }
                next = jarStream.getNextEntry();
            }
        }
        return null;
    }

    public static boolean isRealModule(File jarFile) throws IOException {
        try (JarInputStream jarStream =  new JarInputStream(Files.newInputStream(jarFile.toPath()))) {
            boolean isMultiReleaseJar = containsMultiReleaseJarEntry(jarStream);
            ZipEntry next = jarStream.getNextEntry();
            while (next != null) {
                if (MODULE_INFO_CLASS_FILE.equals(next.getName())) {
                    return true;
                }
                if (isMultiReleaseJar && MODULE_INFO_CLASS_MRJAR_PATH.matcher(next.getName()).matches()) {
                    return true;
                }
                next = jarStream.getNextEntry();
            }
        }
        return false;
    }

    private static String getAutomaticModuleName(Manifest manifest) {
        if (manifest == null) {
            return null;
        }
        return manifest.getMainAttributes().getValue(AUTOMATIC_MODULE_NAME_ATTRIBUTE);
    }

    private static boolean containsMultiReleaseJarEntry(JarInputStream jarStream) {
        Manifest manifest = jarStream.getManifest();
        return manifest !=null && Boolean.parseBoolean(manifest.getMainAttributes().getValue(MULTI_RELEASE_ATTRIBUTE));
    }

    private static String readNameFromModuleInfoClass(InputStream input) throws IOException {
        ClassReader classReader = new ClassReader(input);
        String[] moduleName = new String[1];
        classReader.accept(new ClassVisitor(Opcodes.ASM8) {
            @Override
            public ModuleVisitor visitModule(String name, int access, String version) {
                moduleName[0] = name;
                return super.visitModule(name, access, version);
            }
        }, 0);
        return moduleName[0];
    }
}
