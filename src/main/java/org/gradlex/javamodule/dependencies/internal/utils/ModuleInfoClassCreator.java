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

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ModuleVisitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.objectweb.asm.Opcodes.ACC_MANDATED;
import static org.objectweb.asm.Opcodes.ACC_MODULE;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;

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
