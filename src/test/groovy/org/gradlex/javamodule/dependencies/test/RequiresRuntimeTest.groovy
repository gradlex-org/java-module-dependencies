package org.gradlex.javamodule.dependencies.test

import org.gradle.testkit.runner.TaskOutcome
import org.gradlex.javamodule.dependencies.test.fixture.GradleBuild
import spock.lang.Specification

class RequiresRuntimeTest extends Specification {

    @Delegate
    GradleBuild build = new GradleBuild()

    def "can define runtime only dependencies in module-info"() {
        given:
        appBuildFile << '''
            dependencies.constraints {
                javaModuleDependencies {
                    implementation(gav("org.slf4j", "2.0.3"))
                    implementation(gav("org.slf4j.simple", "2.0.3"))
                }
            }
        '''
        appModuleInfoFile << '''
            module org.gradlex.test.app {
                requires org.slf4j;
                requires /*runtime*/ org.slf4j.simple;
            }
        '''

        when:
        def rt = printRuntimeJars()

        then:
        rt.output.contains('[slf4j-simple-2.0.3.jar, slf4j-api-2.0.3.jar]')

        when:
        def cp = printCompileJars()

        then:
        cp.output.contains('[org.slf4j.simple, slf4j-api-2.0.3.jar]') // 'org.slf4j.simple' is the folder nam of synthetic 'module-info.class'
    }

    def "compiles with runtime only dependencies in module-info"() {
        given:
        appBuildFile << '''
            dependencies.constraints {
                javaModuleDependencies {
                    implementation(gav("org.slf4j", "2.0.3"))
                    implementation(gav("org.slf4j.simple", "2.0.3"))
                }
            }
        '''
        appModuleInfoFile << '''
            module org.gradlex.test.app {
                requires org.slf4j;
                requires /*runtime*/ org.slf4j.simple;
                
                exports org.gradlex.test.app;
            }
        '''
        file("app/src/main/java/org/gradlex/test/app/Main.java") << """
            package org.gradlex.test.app;
            
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            
            public class Main {
                private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
                
                public static void main(String[] args) {
                    LOGGER.info("Running application...");
                }
            }
        """

        when:
        def result = run()

        then:
        result.output.contains("[main] INFO org.gradlex.test.app.Main - Running application...")
    }

    def "runtime only dependencies are not visible at compile time"() {
        given:
        appBuildFile << '''
            dependencies.constraints {
                javaModuleDependencies {
                    implementation(gav("org.slf4j", "2.0.3"))
                }
            }
        '''
        appModuleInfoFile << '''
            module org.gradlex.test.app {
                requires /*runtime*/ org.slf4j;
                
                exports org.gradlex.test.app;
            }
        '''
        file("app/src/main/java/org/gradlex/test/app/Main.java") << """
            package org.gradlex.test.app;
            
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            
            public class Main {
                private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
                
                public static void main(String[] args) {
                    LOGGER.info("Running application...");
                }
            }
        """

        when:
        def result = fail()

        then:
        result.output.contains("error: package org.slf4j does not exist")
    }

    def "generates javadoc with runtime only dependencies in module-info"() {
        given:
        appBuildFile << '''
            java.withJavadocJar()
            dependencies.constraints {
                javaModuleDependencies {
                    implementation(gav("org.slf4j", "2.0.3"))
                    implementation(gav("org.slf4j.simple", "2.0.3"))
                }
            }
        '''
        appModuleInfoFile << '''
            module org.gradlex.test.app {
                requires org.slf4j;
                requires /*runtime*/ org.slf4j.simple;
                
                exports org.gradlex.test.app;
            }
        '''
        file("app/src/main/java/org/gradlex/test/app/Main.java") << """
            package org.gradlex.test.app;
            
            public class Main {}
        """

        when:
        def result = build()

        then:
        result.task(':app:javadoc').outcome == TaskOutcome.SUCCESS
    }

    def "can configure additional compile tasks to work with runtime only dependencies"() {
        // This is typically needed for whitebox testing
        given:
        appBuildFile << '''
            javaModuleDependencies.addRequiresRuntimeSupport(sourceSets.main.get(), sourceSets.test.get())
            tasks.compileTestJava {
                classpath += sourceSets.main.get().output
                
                val srcDir = sourceSets.test.get().java.sourceDirectories.first()
                options.compilerArgumentProviders.add {
                    listOf(
                        "--module-path",
                        classpath.files.joinToString(":"),
                        "--patch-module",
                        "org.gradlex.test.app=" + srcDir
                    )
                }
            }
            
            dependencies.constraints {
                javaModuleDependencies {
                    implementation(gav("org.slf4j", "2.0.3"))
                    implementation(gav("org.slf4j.simple", "2.0.3"))
                }
            }
        '''
        appModuleInfoFile << '''
            module org.gradlex.test.app {
                requires org.slf4j;
                requires /*runtime*/ org.slf4j.simple;
            }
        '''
        file("app/src/test/java/org/gradlex/test/app/MainTest.java") << """
            package org.gradlex.test.app;
            
            public class MainTest {}
        """

        when:
        def result = build()

        then:
        result.task(":app:compileTestJava").outcome == TaskOutcome.SUCCESS
    }
}
