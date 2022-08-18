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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

final class SharedMappings {
    static Map<String, String> mappings = loadModuleNameToGAProperties();

    static Map<String, String> loadModuleNameToGAProperties() {
        Properties properties = new Properties() {
            @Override
            public synchronized Object put(Object key, Object value) {
                if (get(key) != null) {
                    throw new IllegalArgumentException(key + " already present.");
                }
                return super.put(key, value);
            }
        };
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Map<String, String> propertiesAsMap = (Map) properties;

        try (InputStream coordinatesFile = JavaModuleDependenciesExtension.class.getResourceAsStream("unique_modules.properties")) {
            properties.load(coordinatesFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (InputStream coordinatesFile = JavaModuleDependenciesExtension.class.getResourceAsStream("modules.properties")) {
            properties.load(coordinatesFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return propertiesAsMap;
    }

    private SharedMappings() { }
}
