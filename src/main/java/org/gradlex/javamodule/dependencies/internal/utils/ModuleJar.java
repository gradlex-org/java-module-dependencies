// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.internal.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;

public class ModuleJar {
    private static final String AUTOMATIC_MODULE_NAME_ATTRIBUTE = "Automatic-Module-Name";
    private static final String MULTI_RELEASE_ATTRIBUTE = "Multi-Release";
    private static final String MODULE_INFO_CLASS_FILE = "module-info.class";
    private static final Pattern MODULE_INFO_CLASS_MRJAR_PATH =
            Pattern.compile("META-INF/versions/\\d+/module-info.class");

    @Nullable
    public static String readModuleNameFromJarFile(File jarFileOrClassFolder) throws IOException {
        if (jarFileOrClassFolder.isDirectory()) {
            // class folder
            File moduleInfo = new File(jarFileOrClassFolder, MODULE_INFO_CLASS_FILE);
            if (!moduleInfo.exists()) {
                return null;
            }
            return readNameFromModuleInfoClass(Files.newInputStream(moduleInfo.toPath()));
        }
        try (JarInputStream jarStream = new JarInputStream(Files.newInputStream(jarFileOrClassFolder.toPath()))) {
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
                if (isMultiReleaseJar
                        && MODULE_INFO_CLASS_MRJAR_PATH.matcher(next.getName()).matches()) {
                    return readNameFromModuleInfoClass(jarStream);
                }
                next = jarStream.getNextEntry();
            }
        }
        return null;
    }

    public static boolean isRealModule(File jarFileOrClassFolder) throws IOException {
        if (jarFileOrClassFolder.isDirectory()) {
            // class folder
            return new File(jarFileOrClassFolder, MODULE_INFO_CLASS_FILE).exists();
        }
        try (JarInputStream jarStream = new JarInputStream(Files.newInputStream(jarFileOrClassFolder.toPath()))) {
            boolean isMultiReleaseJar = containsMultiReleaseJarEntry(jarStream);
            ZipEntry next = jarStream.getNextEntry();
            while (next != null) {
                if (MODULE_INFO_CLASS_FILE.equals(next.getName())) {
                    return true;
                }
                if (isMultiReleaseJar
                        && MODULE_INFO_CLASS_MRJAR_PATH.matcher(next.getName()).matches()) {
                    return true;
                }
                next = jarStream.getNextEntry();
            }
        }
        return false;
    }

    @Nullable
    private static String getAutomaticModuleName(@Nullable Manifest manifest) {
        if (manifest == null) {
            return null;
        }
        return manifest.getMainAttributes().getValue(AUTOMATIC_MODULE_NAME_ATTRIBUTE);
    }

    private static boolean containsMultiReleaseJarEntry(JarInputStream jarStream) {
        Manifest manifest = jarStream.getManifest();
        return manifest != null
                && Boolean.parseBoolean(manifest.getMainAttributes().getValue(MULTI_RELEASE_ATTRIBUTE));
    }

    private static String readNameFromModuleInfoClass(InputStream input) throws IOException {
        ClassReader classReader = new ClassReader(input);
        String[] moduleName = new String[1];
        classReader.accept(
                new ClassVisitor(Opcodes.ASM8) {
                    @Override
                    public ModuleVisitor visitModule(String name, int access, String version) {
                        moduleName[0] = name;
                        return super.visitModule(name, access, version);
                    }
                },
                0);
        return moduleName[0];
    }
}
