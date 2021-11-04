/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.testing

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.util.Matchers

class VerificationFailureHandlingIntegrationTest extends AbstractIntegrationSpec {

    def setup() {
        // create single project with java plugin
        buildFile << """
            plugins {
              id 'java'
            }

            ${mavenCentralRepository()}

            testing {
                suites {
                    test {
                        useJUnitJupiter()
                    }
                }
            }
        """
    }

    def 'task does not execute when it dependsOn test with doFirst that throws'() {
        given:
        withPassingTest()
        withTestDoFirstActionThrows()
        withCustomTaskDependsOnTestTask()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doFirst action')
    }

    def 'task does not execute when it dependsOn test with doLast that throws'() {
        given:
        withPassingTest()
        withTestDoLastActionThrows()
        withCustomTaskDependsOnTestTask()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doLast action')
    }

    def 'task does not execute when it dependsOn test and VM exits unexpectedly'() {
        given:
        withFatalTestExecutionError()
        withCustomTaskDependsOnTestTask()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        assertFatalTestExecutionError()
    }

    def 'task does not execute when it dependsOn test with failing test(s)'() {
        given:
        withTestVerificationFailure()
        withCustomTaskDependsOnTestTask()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'task does not execute when it dependsOn test with doFirst that throws --continue'() {
        given:
        withPassingTest()
        withTestDoFirstActionThrows()
        withCustomTaskDependsOnTestTask()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doFirst action')
    }

    def 'task does not execute when it dependsOn test with doLast that throws --continue'() {
        given:
        withPassingTest()
        withTestDoLastActionThrows()
        withCustomTaskDependsOnTestTask()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doLast action')
    }

    def 'task does not execute when it dependsOn test and VM exits unexpectedly --continue'() {
        given:
        withFatalTestExecutionError()
        withCustomTaskDependsOnTestTask()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        assertFatalTestExecutionError()
    }

    def 'task does not execute when it dependsOn test with failing test(s) --continue'() {
        given:
        withTestVerificationFailure()
        withCustomTaskDependsOnTestTask()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'task does not execute when it has a test task output dependency and redundant dependsOn test with doFirst that throws'() {
        given:
        withPassingTest()
        withTestDoFirstActionThrows()
        withCustomTaskDependsOnTestTaskAndTestTaskOutput()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doFirst action')
    }

    def 'task does not execute when it has a test task output dependency and redundant dependsOn test and with doLast that throws'() {
        given:
        withPassingTest()
        withTestDoLastActionThrows()
        withCustomTaskDependsOnTestTaskAndTestTaskOutput()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doLast action')
    }

    def 'task does not execute when it has a test task output dependency and redundant dependsOn test and VM exits unexpectedly'() {
        given:
        withFatalTestExecutionError()
        withCustomTaskDependsOnTestTaskAndTestTaskOutput()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        assertFatalTestExecutionError()
    }

    def 'task does not execute when it has a test task output dependency and redundant dependsOn test with failing test(s)'() {
        given:
        withTestVerificationFailure()
        withCustomTaskDependsOnTestTaskAndTestTaskOutput()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'task does not execute when it has a test task output dependency and redundant dependsOn test with doFirst that throws --continue'() {
        given:
        withPassingTest()
        withTestDoFirstActionThrows()
        withCustomTaskDependsOnTestTaskAndTestTaskOutput()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doFirst action')
    }

    def 'task does not execute when it has a test task output dependency and redundant dependsOn test and with doLast that throws --continue'() {
        given:
        withPassingTest()
        withTestDoLastActionThrows()
        withCustomTaskDependsOnTestTaskAndTestTaskOutput()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doLast action')
    }

    def 'task does not execute when it has a test task output dependency and redundant dependsOn test and VM exits unexpectedly --continue'() {
        given:
        withFatalTestExecutionError()
        withCustomTaskDependsOnTestTaskAndTestTaskOutput()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        assertFatalTestExecutionError()
    }

    def 'task does not execute when it has a test task output dependency and redundant dependsOn test with failing test(s) --continue'() {
        given:
        withTestVerificationFailure()
        withCustomTaskDependsOnTestTaskAndTestTaskOutput()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'task does not execute when it has a test task output dependency and test has doFirst that throws'() {
        given:
        withTestVerificationFailure()
        withTestDoFirstActionThrows()
        withCustomTaskInputFromTestTaskOutput()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doFirst action')
    }

    def 'task does not execute when it has a test task output dependency and test has doLast that throws'() {
        given:
        withPassingTest()
        withTestDoLastActionThrows()
        withCustomTaskInputFromTestTaskOutput()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doLast action')
    }

    def 'task does not execute when it has a test task output dependency and VM exits unexpectedly'() {
        given:
        withFatalTestExecutionError()
        withCustomTaskInputFromTestTaskOutput()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        assertFatalTestExecutionError()
    }

    def 'task does not execute when it has a test task output dependency with failing test(s)'() {
        given:
        withTestVerificationFailure()
        withCustomTaskInputFromTestTaskOutput()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'task does not execute when it has a test task output dependency and test has doFirst that throws --continue'() {
        given:
        withTestVerificationFailure()
        withTestDoFirstActionThrows()
        withCustomTaskInputFromTestTaskOutput()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doFirst action')
    }

    def 'task does not execute when it has a test task output dependency and test has doLast that throws --continue'() {
        given:
        withPassingTest()
        withTestDoLastActionThrows()
        withCustomTaskInputFromTestTaskOutput()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doLast action')
    }

    def 'task does not execute when it has a test task output dependency and VM exits unexpectedly --continue'() {
        given:
        withFatalTestExecutionError()
        withCustomTaskInputFromTestTaskOutput()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        assertFatalTestExecutionError()
    }

    def 'task executes when it has a test task output dependency with failing test(s) --continue'() {
        given:
        withTestVerificationFailure()
        withCustomTaskInputFromTestTaskOutput()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'task does not execute when it has a test task output dependency through a transitive task and test has doFirst that throws'() {
        given:
        withTestVerificationFailure()
        withTestDoFirstActionThrows()
        withCustomTaskInputIndirectlyDependsOnTestTaskOutput()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doFirst action')
    }

    def 'task does not execute when it has a test task output dependency through a transitive task and test has doLast that throws'() {
        given:
        withPassingTest()
        withTestDoLastActionThrows()
        withCustomTaskInputIndirectlyDependsOnTestTaskOutput()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doLast action')
    }

    def 'task does not execute when it has a test task output dependency through a transitive task and VM exits unexpectedly'() {
        given:
        withFatalTestExecutionError()
        withCustomTaskInputIndirectlyDependsOnTestTaskOutput()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        assertFatalTestExecutionError()
    }

    def 'task does not execute when it has a test task output dependency through a transitive task with failing test(s)'() {
        given:
        withTestVerificationFailure()
        withCustomTaskInputIndirectlyDependsOnTestTaskOutput()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'task does not execute when it has a test task output dependency through a transitive task and test has doFirst that throws --continue'() {
        given:
        withTestVerificationFailure()
        withTestDoFirstActionThrows()
        withCustomTaskInputIndirectlyDependsOnTestTaskOutput()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doFirst action')
    }

    def 'task does not execute when it has a test task output dependency through a transitive task and test has doLast that throws --continue'() {
        given:
        withPassingTest()
        withTestDoLastActionThrows()
        withCustomTaskInputIndirectlyDependsOnTestTaskOutput()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doLast action')
    }

    def 'task does not execute when it has a test task output dependency through a transitive task and VM exits unexpectedly --continue'() {
        given:
        withFatalTestExecutionError()
        withCustomTaskInputIndirectlyDependsOnTestTaskOutput()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        assertFatalTestExecutionError()
    }

    def 'task does not execute when it has a test task output dependency through a transitive task with failing test(s) --continue'() {
        given:
        withTestVerificationFailure()
        withCustomTaskInputIndirectlyDependsOnTestTaskOutput()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertTestsFailed()
    }

    // helpers

    def withPassingTest() {
        file('src/test/java/example/PassingUnitTest.java').java '''
            package example;

            import org.junit.jupiter.api.Test;

            import static org.junit.jupiter.api.Assertions.assertTrue;

            public class PassingUnitTest {
                @Test
                public void unitTest() {
                    assertTrue(true);
                }
            }
        '''
    }

    /**
     * Cause the test VM to fail at startup by providing an invalid JVM argument.
     */
    def withFatalTestExecutionError() {
        executer.withStackTraceChecksDisabled() // ignore additional ST only seen on Windows
        withPassingTest()
        buildFile << '''
            tasks.named('test', Test).configure {
                jvmArgs '-XX:UnknownArgument'
            }
        '''
    }

    void assertFatalTestExecutionError() {
        failure.assertThatCause(Matchers.matchesRegexp("Process 'Gradle Test Executor \\d+' finished with non-zero exit value \\d+"))
    }

    def withTestVerificationFailure() {
        file('src/test/java/example/UnitTestWithVerificationFailure.java').java '''
            package example;

            import org.junit.jupiter.api.Test;

            import static org.junit.jupiter.api.Assertions.fail;

            public class UnitTestWithVerificationFailure {
                @Test
                public void unitTest() {
                    fail("intentional verification failure");
                }
            }
        '''
    }

    def withCustomTaskDependsOnTestTask() {
        buildFile << '''
            tasks.register('customTask') {
                dependsOn tasks.named('test', Test)
            }
        '''
    }

    def withCustomTaskInputFromTestTaskOutput() {
        buildFile << '''
            abstract class CustomTask extends DefaultTask {

                @InputFiles
                abstract ConfigurableFileCollection getCustomInput()

                @TaskAction
                public void doAction() {
                    // no-op
                }
            }

            def testTask = tasks.named('test', Test)

            tasks.register('customTask', CustomTask) {
                customInput.from(testTask.flatMap { it.binaryResultsDirectory })
            }
        '''
    }

    /**
     * Helper method to setup a custom task wired to the test in two ways:
     * <ol>
     *     <li>A direct dependency via dependsOn declarartion</li>
     *     <li>Via Test#binaryResultsDirectory output wired to customTask#customInput</li>
     * </ol>
     *
     * This intentional redundancy is to test automatic exclusion of direct dependsOn relationships.
     */
    def withCustomTaskDependsOnTestTaskAndTestTaskOutput() {
        withCustomTaskInputFromTestTaskOutput()
        buildFile << '''
            tasks.named('customTask', CustomTask).configure {
                dependsOn tasks.named('test', Test)
            }
        '''
    }

    def withCustomTaskInputIndirectlyDependsOnTestTaskOutput() {
        buildFile << '''
            abstract class CustomTask extends DefaultTask {

                @InputFiles
                abstract ConfigurableFileCollection getCustomInput()

                @TaskAction
                void doAction() {
                    // no-op
                    getLogger().lifecycle("running {}", getPath())
                }
            }

            abstract class IntermediateTask extends DefaultTask {
                @OutputDirectory
                abstract DirectoryProperty getOutputDir()

                @TaskAction
                void doAction() {
                    // no-op
                    getLogger().lifecycle("running {}", getPath())
                }
            }

            def testTask = tasks.named('test', Test)

            def intermediateTask = tasks.register('intermediateTask', IntermediateTask) {
                dependsOn testTask
                outputDir.set(layout.buildDirectory.dir("intermediateTaskOutput"))
            }

            tasks.register('customTask', CustomTask) {
                customInput.from(intermediateTask.flatMap { it.outputDir })
                dependsOn intermediateTask
            }
        '''
    }

    def withTestDoFirstActionThrows() {
        buildFile << '''
            tasks.named('test', Test).configure {
                doFirst {
                    throw new RuntimeException('intentional failure in Test#doFirst action')
                }
            }
        '''
    }

    def withTestDoLastActionThrows() {
        buildFile << '''
            tasks.named('test', Test).configure {
                doLast {
                    throw new RuntimeException('intentional failure in Test#doLast action')
                }
            }
        '''
    }


}
