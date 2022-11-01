package org.gradlex.javamodule.dependencies.test

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
        cp.output.contains('[slf4j-api-2.0.3.jar]')
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
}
