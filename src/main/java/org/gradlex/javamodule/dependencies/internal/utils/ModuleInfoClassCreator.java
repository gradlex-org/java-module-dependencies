// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.dependencies.internal.utils;

import static org.objectweb.asm.Opcodes.ACC_MANDATED;
import static org.objectweb.asm.Opcodes.ACC_MODULE;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ModuleVisitor;

public abstract class ModuleInfoClassCreator {

    public static void createEmpty(File targetFolder) {
        //noinspection ResultOfMethodCallIgnored
        targetFolder.mkdirs();

        ClassWriter cw = new ClassWriter(0);
        cw.visit(53, ACC_MODULE, "module-info", null, null, null);
        ModuleVisitor moduleVisitor = cw.visitModule(targetFolder.getName(), ACC_SYNTHETIC, null);
        moduleVisitor.visitRequire("java.base", ACC_MANDATED, null);
        cw.visitEnd();
        try (FileOutputStream s = new FileOutputStream(new File(targetFolder, "module-info.class"))) {
            s.write(cw.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
