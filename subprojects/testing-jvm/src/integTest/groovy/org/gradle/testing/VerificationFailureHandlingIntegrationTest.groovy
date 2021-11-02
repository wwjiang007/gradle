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

    def 'custom task depends on Test task, no --continue; Test executes with passing test(s)'() {
        given:
        withPassingTest()
        withCustomTaskDependsOnTestTaskButDoesNotHandleVerificationFailures()

        expect:
        succeeds('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskExecuted(':customTask')
    }

    def 'custom task depends on Test task, no --continue; Test throws from doFirst action'() {
        given:
        withPassingTest()
        withTestDoFirstActionThrows()
        withCustomTaskDependsOnTestTaskButDoesNotHandleVerificationFailures()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doFirst action')
    }

    def 'custom task depends on Test task, no --continue; Test throws from doLast action'() {
        given:
        withPassingTest()
        withTestDoLastActionThrows()
        withCustomTaskDependsOnTestTaskButDoesNotHandleVerificationFailures()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doLast action')
    }

    def 'custom task depends on Test task, no --continue; Test exits VM early'() {
        given:
        withFatalTestExecutionError()
        withCustomTaskDependsOnTestTaskButDoesNotHandleVerificationFailures()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        assertFatalTestExecutionError()
    }

    def 'custom task depends on Test task, no --continue; Test has failing test(s)'() {
        given:
        withTestVerificationFailure()
        withCustomTaskDependsOnTestTaskButDoesNotHandleVerificationFailures()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'custom task depends on Test task, with --continue; Test executes with passing test(s)'() {
        given:
        withPassingTest()
        withCustomTaskDependsOnTestTaskButDoesNotHandleVerificationFailures()

        expect:
        succeeds('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskExecuted(':customTask')
    }

    def 'custom task depends on Test task, with --continue; Test throws from doFirst action'() {
        given:
        withPassingTest()
        withTestDoFirstActionThrows()
        withCustomTaskDependsOnTestTaskButDoesNotHandleVerificationFailures()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doFirst action')
    }

    def 'custom task depends on Test task, with --continue; Test throws from doLast action'() {
        given:
        withPassingTest()
        withTestDoLastActionThrows()
        withCustomTaskDependsOnTestTaskButDoesNotHandleVerificationFailures()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doLast action')
    }

    def 'custom task depends on Test task, with --continue; Test exits VM early'() {
        given:
        withFatalTestExecutionError()
        withCustomTaskDependsOnTestTaskButDoesNotHandleVerificationFailures()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        assertFatalTestExecutionError()
    }

    def 'custom task depends on Test task, with --continue; Test has failing test(s)'() {
        given:
        withTestVerificationFailure()
        withCustomTaskDependsOnTestTaskButDoesNotHandleVerificationFailures()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'custom task depends on Test task and handles verification failures, no --continue; Test executes with passing test(s)'() {
        given:
        withPassingTest()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailures()

        expect:
        succeeds('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskExecuted(':customTask')
    }

    def 'custom task depends on Test task and handles verification failures, no --continue; Test throws from doFirst action'() {
        given:
        withPassingTest()
        withTestDoFirstActionThrows()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailures()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doFirst action')
    }

    def 'custom task depends on Test task and handles verification failures, no --continue; Test throws from doLast action'() {
        given:
        withPassingTest()
        withTestDoLastActionThrows()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailures()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doLast action')
    }

    def 'custom task depends on Test task and handles verification failures, no --continue; Test exits VM early'() {
        given:
        withFatalTestExecutionError()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailures()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        assertFatalTestExecutionError()
    }

    def 'custom task depends on Test task and handles verification failures, no --continue; Test has failing test(s)'() {
        given:
        withTestVerificationFailure()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailures()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'custom task depends on Test task and handles verification failures, with --continue; Test executes with passing test(s)'() {
        given:
        withPassingTest()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailures()

        expect:
        succeeds('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskExecuted(':customTask')
    }

    def 'custom task depends on Test task and handles verification failures, with --continue; Test throws from doFirst action'() {
        given:
        withPassingTest()
        withTestDoFirstActionThrows()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailures()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask') // TODO bug?
        failure.assertHasCause('intentional failure in Test#doFirst action')
    }

    def 'custom task depends on Test task and handles verification failures, with --continue; Test throws from doLast action'() {
        given:
        withPassingTest()
        withTestDoLastActionThrows()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailures()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doLast action')
    }

    def 'custom task depends on Test task and handles verification failures, with --continue; Test exits VM early'() {
        given:
        withFatalTestExecutionError()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailures()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        assertFatalTestExecutionError()
    }

    def 'custom task depends on Test task and handles verification failures, with --continue; Test has failing test(s)'() {
        given:
        withTestVerificationFailure()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailures()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'custom task with dependency on Test via an input property marked with the new annotation, no --continue; Test executes with passing test(s)'() {
        given:
        withPassingTest()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaInputProperty()

        expect:
        succeeds('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskExecuted(':customTask')
    }

    def 'custom task with dependency on Test via an input property marked with the new annotation, no --continue; Test throws from doFirst action'() {
        given:
        withTestVerificationFailure()
        withTestDoFirstActionThrows()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaInputProperty()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doFirst action')
    }

    def 'custom task with dependency on Test via an input property marked with the new annotation, no --continue; Test throws from doLast action'() {
        given:
        withTestVerificationFailure()
        withTestDoLastActionThrows()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaInputProperty()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'custom task with dependency on Test via an input property marked with the new annotation, no --continue; Test exits VM early'() {
        given:
        withFatalTestExecutionError()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaInputProperty()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        assertFatalTestExecutionError()
    }

    def 'custom task with dependency on Test via an input property marked with the new annotation, no --continue; Test has failing test(s)'() {
        given:
        withTestVerificationFailure()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaInputProperty()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'custom task with dependency on Test via an input property marked with the new annotation, with --continue; Test executes with passing test(s)'() {
        given:
        withPassingTest()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaInputProperty()

        expect:
        succeeds('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskExecuted(':customTask')
    }

    def 'custom task with dependency on Test via an input property marked with the new annotation, with --continue; Test throws from doFirst action'() {
        given:
        withTestVerificationFailure()
        withTestDoFirstActionThrows()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaInputProperty()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask') // TODO bug?
        failure.assertHasCause('intentional failure in Test#doFirst action')
    }

    def 'custom task with dependency on Test via an input property marked with the new annotation, with --continue; Test throws from doLast action'() {
        given:
        withTestVerificationFailure()
        withTestDoLastActionThrows()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaInputProperty()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'custom task with dependency on Test via an input property marked with the new annotation, with --continue; Test exits VM early'() {
        given:
        withFatalTestExecutionError()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaInputProperty()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        assertFatalTestExecutionError()
    }

    def 'custom task with dependency on Test via an input property marked with the new annotation, with --continue; Test has failing test(s)'() {
        given:
        withTestVerificationFailure()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaInputProperty()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'custom task with dependency on Test via an input property that _does not_ have the new annotation, no --continue; Test executes with passing test(s)'() {
        given:
        withPassingTest()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaUnrelatedProperty()

        expect:
        succeeds('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskExecuted(':customTask')
    }

    def 'custom task with dependency on Test via an input property that _does not_ have the new annotation, no --continue; Test throws from doFirst action'() {
        given:
        withTestVerificationFailure()
        withTestDoFirstActionThrows()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaUnrelatedProperty()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doFirst action')
    }

    def 'custom task with dependency on Test via an input property that _does not_ have the new annotation, no --continue; Test throws from doLast action'() {
        given:
        withTestVerificationFailure()
        withTestDoLastActionThrows()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaUnrelatedProperty()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'custom task with dependency on Test via an input property that _does not_ have the new annotation, no --continue; Test exits VM early'() {
        given:
        withFatalTestExecutionError()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaUnrelatedProperty()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        assertFatalTestExecutionError()
    }

    def 'custom task with dependency on Test via an input property that _does not_ have the new annotation, no --continue; Test has failing test(s)'() {
        given:
        withTestVerificationFailure()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaUnrelatedProperty()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'custom task with dependency on Test via an input property that _does not_ have the new annotation, with --continue; Test executes with passing test(s)'() {
        given:
        withPassingTest()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaUnrelatedProperty()

        expect:
        succeeds('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskExecuted(':customTask')
    }

    def 'custom task with dependency on Test via an input property that _does not_ have the new annotation, with --continue; Test throws from doFirst action'() {
        given:
        withTestVerificationFailure()
        withTestDoFirstActionThrows()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaUnrelatedProperty()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask') // TODO bug?
        failure.assertHasCause('intentional failure in Test#doFirst action')
    }

    def 'custom task with dependency on Test via an input property that _does not_ have the new annotation, with --continue; Test throws from doLast action'() {
        given:
        withTestVerificationFailure()
        withTestDoLastActionThrows()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaUnrelatedProperty()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'custom task with dependency on Test via an input property that _does not_ have the new annotation, with --continue; Test exits VM early'() {
        given:
        withFatalTestExecutionError()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaUnrelatedProperty()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        assertFatalTestExecutionError()
    }

    def 'custom task with dependency on Test via an input property that _does not_ have the new annotation, with --continue; Test has failing test(s)'() {
        given:
        withTestVerificationFailure()
        withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaUnrelatedProperty()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'custom task with transitive dependency on Test via another task, no --continue; Test executes with passing test(s)'() {
        given:
        withPassingTest()
        withCustomTaskIndirectlyDependsOnTestTaskAndHandlesVerificationFailures()

        expect:
        succeeds('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskExecuted(':customTask')
    }

    def 'custom task with transitive dependency on Test via another task, no --continue; Test throws from doFirst action'() {
        given:
        withTestVerificationFailure()
        withTestDoFirstActionThrows()
        withCustomTaskIndirectlyDependsOnTestTaskAndHandlesVerificationFailures()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doFirst action')
    }

    def 'custom task with transitive dependency on Test via another task, no --continue; Test throws from doLast action'() {
        given:
        withTestVerificationFailure()
        withTestDoLastActionThrows()
        withCustomTaskIndirectlyDependsOnTestTaskAndHandlesVerificationFailures()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'custom task with transitive dependency on Test via another task, no --continue; Test exits VM early'() {
        given:
        withFatalTestExecutionError()
        withCustomTaskIndirectlyDependsOnTestTaskAndHandlesVerificationFailures()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        assertFatalTestExecutionError()
    }

    def 'custom task with transitive dependency on Test via another task, no --continue; Test has failing test(s)'() {
        given:
        withTestVerificationFailure()
        withCustomTaskIndirectlyDependsOnTestTaskAndHandlesVerificationFailures()

        expect:
        fails('customTask')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'custom task with transitive dependency on Test via another task, with --continue; Test executes with passing test(s)'() {
        given:
        withPassingTest()
        withCustomTaskIndirectlyDependsOnTestTaskAndHandlesVerificationFailures()

        expect:
        succeeds('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskExecuted(':customTask')
    }

    def 'custom task with transitive dependency on Test via another task, with --continue; Test throws from doFirst action'() {
        given:
        withTestVerificationFailure()
        withTestDoFirstActionThrows()
        withCustomTaskIndirectlyDependsOnTestTaskAndHandlesVerificationFailures()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertHasCause('intentional failure in Test#doFirst action')
    }

    def 'custom task with transitive dependency on Test via another task, with --continue; Test throws from doLast action'() {
        given:
        withTestVerificationFailure()
        withTestDoLastActionThrows()
        withCustomTaskIndirectlyDependsOnTestTaskAndHandlesVerificationFailures()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        failure.assertTestsFailed()
    }

    def 'custom task with transitive dependency on Test via another task, with --continue; Test exits VM early'() {
        given:
        withFatalTestExecutionError()
        withCustomTaskIndirectlyDependsOnTestTaskAndHandlesVerificationFailures()

        expect:
        fails('customTask', '--continue')
        result.assertTaskExecuted(':test')
        result.assertTaskNotExecuted(':customTask')
        assertFatalTestExecutionError()
    }

    def 'custom task with transitive dependency on Test via another task, with --continue; Test has failing test(s)'() {
        given:
        withTestVerificationFailure()
        withCustomTaskIndirectlyDependsOnTestTaskAndHandlesVerificationFailures()

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

    def withFatalTestExecutionError() {
        file('src/test/java/example/UnitTestWithFatalExecutionError.java').java '''
            package example;

            import org.junit.jupiter.api.Test;

            public class UnitTestWithFatalExecutionError {
                @Test
                public void unitTest() {
                    System.exit(42); // prematurely exit the testing VM
                }
            }
        '''
    }

    void assertFatalTestExecutionError() {
        failure.assertThatCause(Matchers.matchesRegexp("Process 'Gradle Test Executor \\d+' finished with non-zero exit value.*"))
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

    def withCustomTaskDependsOnTestTaskButDoesNotHandleVerificationFailures() {
        buildFile << '''
            tasks.register('customTask') {
                dependsOn tasks.named('test', Test)
            }
        '''
    }

    def withCustomTaskDependsOnTestTaskAndHandlesVerificationFailures() {
        buildFile << '''
            abstract class CustomTask extends DefaultTask {

                @InputFiles
                @HandlesVerificationFailures
                abstract ConfigurableFileCollection getCustomInput()

                @TaskAction
                public void doAction() {
                    // no-op
                }
            }


            tasks.register('customTask', CustomTask) {
                dependsOn tasks.named('test', Test)
            }
        '''
    }

    def withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaInputProperty() {
        buildFile << '''
            abstract class CustomTask extends DefaultTask {

                @InputFiles
                @HandlesVerificationFailures
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

    def withCustomTaskDependsOnTestTaskAndHandlesVerificationFailuresViaUnrelatedProperty() {
        buildFile << '''
            abstract class CustomTask extends DefaultTask {

                @InputFiles
                abstract ConfigurableFileCollection getCustomInput()

                /**
                 * this input is unrelated to the test task
                 *
                 * however, because it is annotated with @HandlesVerificationFailures, it will (for now) allow CustomTask to execute
                 * even when the source of customInput (wired to the test task output) fails with a verification error
                 */
                @InputFiles
                @HandlesVerificationFailures
                abstract ConfigurableFileCollection getAnotherInput()

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

    def withCustomTaskIndirectlyDependsOnTestTaskAndHandlesVerificationFailures() {
        buildFile << '''
            abstract class CustomTask extends DefaultTask {

                @InputFiles
                @HandlesVerificationFailures
                abstract ConfigurableFileCollection getCustomInput()

                @TaskAction
                void doAction() {
                    // no-op
                    getLogger().lifecycle("running {}", getPath())
                }
            }

            abstract class IntermediateTask extends DefaultTask {
                @InputFiles
                //@HandlesVerificationFailures // IntermediateTask will not run without this
                abstract ConfigurableFileCollection getIntermediateInput()

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
                intermediateInput.from(testTask.flatMap { it.binaryResultsDirectory }) //get().binaryResultsDirectory/*.convention('binaryResultsDir')*/)
                outputDir.set(layout.buildDirectory.dir("intermediateTaskOutput"))
            }

            tasks.register('customTask', CustomTask) {
                customInput.from(intermediateTask.flatMap { it.outputDir }) //get().outputDir)
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
