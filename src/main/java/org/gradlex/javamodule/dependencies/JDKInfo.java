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

package org.gradlex.javamodule.dependencies;

import java.util.Arrays;
import java.util.List;

interface JDKInfo {
    List<String> MODULES = Arrays.asList(
            "java.base",
            "java.compiler",
            "java.datatransfer",
            "java.desktop",
            "java.instrument",
            "java.logging",
            "java.management",
            "java.management.rmi",
            "java.naming",
            "java.net.http",
            "java.prefs",
            "java.rmi",
            "java.scripting",
            "java.se",
            "java.security.jgss",
            "java.security.sasl",
            "java.smartcardio",
            "java.sql",
            "java.sql.rowset",
            "java.transaction.xa",
            "java.xml",
            "java.xml.crypto",
            "jdk.accessibility",
            "jdk.attach",
            "jdk.charsets",
            "jdk.compiler",
            "jdk.crypto.cryptoki",
            "jdk.crypto.ec",
            "jdk.dynalink",
            "jdk.editpad",
            "jdk.hotspot.agent",
            "jdk.httpserver",
            "jdk.incubator.foreign",
            "jdk.incubator.vector",
            "jdk.jartool",
            "jdk.javadoc",
            "jdk.jcmd",
            "jdk.jconsole",
            "jdk.jdeps",
            "jdk.jdi",
            "jdk.jdwp.agent",
            "jdk.jfr",
            "jdk.jlink",
            "jdk.jpackage",
            "jdk.jshell",
            "jdk.jsobject",
            "jdk.jstatd",
            "jdk.localedata",
            "jdk.management",
            "jdk.management.agent",
            "jdk.management.jfr",
            "jdk.naming.dns",
            "jdk.naming.rmi",
            "jdk.net",
            "jdk.nio.mapmode",
            "jdk.sctp",
            "jdk.security.auth",
            "jdk.security.jgss",
            "jdk.xml.dom",
            "jdk.zipfs"
    );
}
