/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.integtests.internal.component

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.ToBeFixedForConfigurationCache
import org.gradle.internal.component.resolution.failure.exception.AbstractResolutionFailureException
import org.gradle.internal.component.resolution.failure.exception.ArtifactSelectionException
import org.gradle.internal.component.resolution.failure.exception.GraphValidationException
import org.gradle.internal.component.resolution.failure.exception.VariantSelectionByNameException
import org.gradle.internal.component.resolution.failure.exception.VariantSelectionByAttributesException
import org.gradle.internal.component.resolution.failure.type.AmbiguousArtifactTransformsFailure
import org.gradle.internal.component.resolution.failure.type.AmbiguousArtifactsFailure
import org.gradle.internal.component.resolution.failure.type.NoCompatibleArtifactFailure
import org.gradle.internal.component.resolution.failure.type.NoCompatibleVariantsFailure
import org.gradle.internal.component.resolution.failure.type.IncompatibleMultipleNodesValidationFailure
import org.gradle.internal.component.resolution.failure.type.ConfigurationNotCompatibleFailure
import org.gradle.internal.component.resolution.failure.type.ConfigurationDoesNotExistFailure
import org.gradle.internal.component.resolution.failure.interfaces.ResolutionFailure
import org.gradle.internal.component.resolution.failure.type.AmbiguousVariantsFailure
import org.gradle.test.fixtures.dsl.GradleDsl
import org.gradle.test.fixtures.file.TestFile
import org.gradle.util.GradleVersion

/**
 * These tests demonstrate the behavior of the [ResolutionFailureHandler] when a project has various
 * variant selection failures.
 *
 * It can also build a text report demonstrating all these errors in a single place by running
 * the [generateFailureShowcase] method, which is marked with [spock.lang.Ignore] so it doesn't
 * run as part of a typical test run.  It is useful for viewing and comparing the behavior of
 * different types of failures.
 */
class ResolutionFailureHandlerIntegrationTest extends AbstractIntegrationSpec {

    // region Stage 2 - VariantSelectionFailure
    def "demonstrate ambiguous graph variant selection failure with single disambiguating value for project"() {
        ambiguousGraphVariantForProjectWithSingleDisambiguatingAttribute.prepare()

        expect:
        assertResolutionFailsAsExpected(ambiguousGraphVariantForProjectWithSingleDisambiguatingAttribute)

        and: "Has error output"
        failure.assertHasDescription("Could not determine the dependencies of task ':forceResolution'.")
        failure.assertHasCause("Could not resolve all dependencies for configuration ':resolveMe'.")
        failure.assertHasCause("Could not resolve project :.")
        assertFullMessageCorrect("""      > The consumer was configured to find attribute 'color' with value 'blue'. There are several available matching variants of project :
        The only attribute distinguishing these variants is 'shape'. Add this attribute to the consumer's configuration to resolve the ambiguity:
          - Value: 'round' selects variant: 'blueRoundElements'
          - Value: 'square' selects variant: 'blueSquareElements'
          - Value: 'triangle' selects variant: 'blueTriangleElements'""")
        and: "Helpful resolutions are provided"
        assertSuggestsReviewingAlgorithm()
        assertSuggestsViewingDocs("Ambiguity errors are explained in more detail at https://docs.gradle.org/${GradleVersion.current().version}/userguide/variant_model.html#sub:variant-ambiguity.")
        assertSuggestsViewingDocs("Use the dependencyInsight report with the --all-variants option to view all variants of the ambiguous dependency.  This report is described at https://docs.gradle.org/${GradleVersion.current().version}/userguide/viewing_debugging_dependencies.html#sec:identifying_reason_dependency_selection.")
    }

    def "demonstrate ambiguous graph variant without single disambiguating value selection failure for project"() {
        ambiguousGraphVariantForProjectWithoutSingleDisambiguatingAttribute.prepare()

        expect:
        assertResolutionFailsAsExpected(ambiguousGraphVariantForProjectWithSingleDisambiguatingAttribute)

        and: "Has error output"
        failure.assertHasDescription("Could not determine the dependencies of task ':forceResolution'.")
        failure.assertHasCause("Could not resolve all dependencies for configuration ':resolveMe'.")
        failure.assertHasCause("Could not resolve project :.")
        assertFullMessageCorrect("""      > The consumer was configured to find attribute 'color' with value 'blue'. However we cannot choose between the following variants of project ::
          - blueRoundTransparentElements
          - blueSquareOpaqueElements
          - blueSquareTransparentElements
        All of them match the consumer attributes:
          - Variant 'blueRoundTransparentElements' capability ':${temporaryFolder.getTestDirectory().getName()}:unspecified' declares attribute 'color' with value 'blue':
              - Unmatched attributes:
                  - Provides opacity 'transparent' but the consumer didn't ask for it
                  - Provides shape 'round' but the consumer didn't ask for it
          - Variant 'blueSquareOpaqueElements' capability ':${temporaryFolder.getTestDirectory().getName()}:unspecified' declares attribute 'color' with value 'blue':
              - Unmatched attributes:
                  - Provides opacity 'opaque' but the consumer didn't ask for it
                  - Provides shape 'square' but the consumer didn't ask for it
          - Variant 'blueSquareTransparentElements' capability ':${temporaryFolder.getTestDirectory().getName()}:unspecified' declares attribute 'color' with value 'blue':
              - Unmatched attributes:
                  - Provides opacity 'transparent' but the consumer didn't ask for it
                  - Provides shape 'square' but the consumer didn't ask for it""")
        and: "Helpful resolutions are provided"
        assertSuggestsReviewingAlgorithm()
        assertSuggestsViewingDocs("Ambiguity errors are explained in more detail at https://docs.gradle.org/${GradleVersion.current().version}/userguide/variant_model.html#sub:variant-ambiguity.")
    }

    @ToBeFixedForConfigurationCache
    def "demonstrate ambiguous graph variant selection failure with single disambiguating value for externalDep"() {
        ambiguousGraphVariantForExternalDep.prepare()

        expect:
        assertResolutionFailsAsExpected(ambiguousGraphVariantForExternalDep)

        and: "Has error output"
        failure.assertHasDescription("Execution failed for task ':forceResolution'")
        failure.assertHasCause("Could not resolve all files for configuration ':resolveMe'.")
        failure.assertHasCause("Could not resolve com.squareup.okhttp3:okhttp:4.4.0.")
        assertFullMessageCorrect("""   > Could not resolve com.squareup.okhttp3:okhttp:4.4.0.
     Required by:
         project :
      > The consumer was configured to find attribute 'org.gradle.category' with value 'documentation'. There are several available matching variants of com.squareup.okhttp3:okhttp:4.4.0
        The only attribute distinguishing these variants is 'org.gradle.docstype'. Add this attribute to the consumer's configuration to resolve the ambiguity:
          - Value: 'javadoc' selects variant: 'javadocElements'
          - Value: 'sources' selects variant: 'sourcesElements'""")

        and: "Helpful resolutions are provided"
        assertSuggestsReviewingAlgorithm()
        assertSuggestsViewingDocs("Ambiguity errors are explained in more detail at https://docs.gradle.org/${GradleVersion.current().version}/userguide/variant_model.html#sub:variant-ambiguity.")
    }

    def "demonstrate no matching graph variants selection failure for project"() {
        noMatchingGraphVariantsForProject.prepare()

        expect:
        assertResolutionFailsAsExpected(noMatchingGraphVariantsForProject)

        and: "Has error output"
        failure.assertHasDescription("Could not determine the dependencies of task ':forceResolution'.")
        failure.assertHasCause("Could not resolve all dependencies for configuration ':resolveMe'.")
        failure.assertHasCause("Could not resolve project :.")
        assertFullMessageCorrect("""   > Could not resolve project :.
     Required by:
         project :
      > No matching variant of project : was found. The consumer was configured to find attribute 'color' with value 'green' but:
          - Variant 'default':
              - Incompatible because this component declares attribute 'color' with value 'blue' and the consumer needed attribute 'color' with value 'green'""")

        and: "Helpful resolutions are provided"
        assertSuggestsReviewingAlgorithm()
        assertSuggestsViewingDocs("No matching variant errors are explained in more detail at https://docs.gradle.org/${GradleVersion.current().version}/userguide/variant_model.html#sub:variant-no-match.")
    }

    @ToBeFixedForConfigurationCache
    def "demonstrate no matching graph variants selection failure for externalDep"() {
        noMatchingGraphVariantsForExternalDep.prepare()

        expect:
        assertResolutionFailsAsExpected(noMatchingGraphVariantsForExternalDep)

        and: "Has error output"
        failure.assertHasDescription("Execution failed for task ':forceResolution'.")
        failure.assertHasCause("Could not resolve all files for configuration ':resolveMe'.")
        failure.assertHasCause("Could not resolve com.squareup.okhttp3:okhttp:4.4.0.")
        assertFullMessageCorrect("""      > No matching variant of com.squareup.okhttp3:okhttp:4.4.0 was found. The consumer was configured to find attribute 'org.gradle.category' with value 'non-existent-format' but:
          - Variant 'apiElements':
              - Incompatible because this component declares attribute 'org.gradle.category' with value 'library' and the consumer needed attribute 'org.gradle.category' with value 'non-existent-format'
          - Variant 'javadocElements':
              - Incompatible because this component declares attribute 'org.gradle.category' with value 'documentation' and the consumer needed attribute 'org.gradle.category' with value 'non-existent-format'
          - Variant 'runtimeElements':
              - Incompatible because this component declares attribute 'org.gradle.category' with value 'library' and the consumer needed attribute 'org.gradle.category' with value 'non-existent-format'
          - Variant 'sourcesElements':
              - Incompatible because this component declares attribute 'org.gradle.category' with value 'documentation' and the consumer needed attribute 'org.gradle.category' with value 'non-existent-format'""")

        and: "Helpful resolutions are provided"
        assertSuggestsReviewingAlgorithm()
        assertSuggestsViewingDocs("No matching variant errors are explained in more detail at https://docs.gradle.org/${GradleVersion.current().version}/userguide/variant_model.html#sub:variant-no-match.")
    }

    def 'demonstrate incompatible requested configuration failure'() {
        incompatibleRequestedConfiguration.prepare()

        expect:
        assertResolutionFailsAsExpected(incompatibleRequestedConfiguration)

        and: "Has error output"
        failure.assertHasDescription("Could not determine the dependencies of task ':forceResolution'.")
        failure.assertHasCause("Could not resolve all dependencies for configuration ':resolveMe'.")
        failure.assertHasCause("Could not resolve project :.")
        assertFullMessageCorrect("""     Required by:
         project :
      > Configuration 'mismatch' in project : does not match the consumer attributes
        Configuration 'mismatch':
          - Incompatible because this component declares attribute 'color' with value 'blue' and the consumer needed attribute 'color' with value 'green'
""")

        and: "Helpful resolutions are provided"
        assertSuggestsReviewingAlgorithm()
        assertSuggestsViewingDocs("Incompatible variant errors are explained in more detail at https://docs.gradle.org/${GradleVersion.current().version}/userguide/variant_model.html#sub:variant-incompatible.")
    }

    def "demonstrate no variants exist"() {
        noGraphVariantsExistForProject.prepare()

        expect:
        assertResolutionFailsAsExpected(noGraphVariantsExistForProject)

        and: "Has error output"
        failure.assertHasDescription("Could not determine the dependencies of task ':forceResolution'.")
        failure.assertHasCause("Could not resolve all dependencies for configuration ':resolveMe'.")
        failure.assertHasCause("Could not resolve project :producer.")
        assertFullMessageCorrect("""     Required by:
         project :
      > No matching variant of project :producer was found. The consumer was configured to find attribute 'color' with value 'green' but:
          - No variants exist.""")

        and: "Helpful resolutions are provided"
        assertSuggestsReviewingAlgorithm()
        assertSuggestsViewingDocs("Creating consumable variants is explained in more detail at https://docs.gradle.org/${GradleVersion.current().version}/userguide/declaring_dependencies.html#sec:resolvable-consumable-configs.")
    }

    def "demonstrate configuration not found selection failure"() {
        configurationNotFound.prepare()

        expect:
        assertResolutionFailsAsExpected(configurationNotFound)

        and: "Has error output"
        failure.assertHasDescription("Could not determine the dependencies of task ':forceResolution'.")
        failure.assertHasCause("Could not resolve all dependencies for configuration ':resolveMe'.")
        failure.assertHasCause("Could not resolve project :.")
        assertFullMessageCorrect("""Required by:
         project :
      > A dependency was declared on configuration 'absent' of 'project :' but no variant with that configuration name exists.""")

        and: "Helpful resolutions are provided"
        assertSuggestsReviewingAlgorithm()
        // TODO: Nothing specific here
    }
    // endregion Stage 2 - VariantSelectionFailure

    // region Stage 3 - GraphValidationFailure
    // endregion Stage 3 - GraphValidationFailure

    // region Stage 4 - ArtifactSelectionFailure
    def "demonstrate incompatible artifact variants exception"() {
        incompatibleArtifactVariants.prepare()

        expect:
        assertResolutionFailsAsExpected(incompatibleArtifactVariants)

        and: "Has error output"
        failure.assertHasDescription("Could not determine the dependencies of task ':forceResolution'.")
        failure.assertHasCause("Could not resolve all dependencies for configuration ':resolveMe'.")
        failure.assertHasCause("Could not resolve project :.")
        assertFullMessageCorrect("""     Required by:
         project :
      > Multiple incompatible variants of org.example:${temporaryFolder.getTestDirectory().getName()}:1.0 were selected:
           - Variant blueElementsCapability1 has attributes {color=blue}
           - Variant greenElementsCapability2 has attributes {color=green}""")

        and: "Helpful resolutions are provided"
        assertSuggestsReviewingAlgorithm()
        assertSuggestsViewingDocs("Incompatible variant errors are explained in more detail at https://docs.gradle.org/${GradleVersion.current().version}/userguide/variant_model.html#sub:variant-incompatible.")
    }

    def "demonstrate no matching artifact variants exception"() {
        noMatchingArtifactVariants.prepare()

        expect:
        assertResolutionFailsAsExpected(noMatchingArtifactVariants)

        and: "Has error output"
        failure.assertHasDescription("Could not determine the dependencies of task ':forceResolution'.")
        failure.assertHasCause("Could not resolve all dependencies for configuration ':resolveMe'.")
        assertFullMessageCorrect("""   > No variants of project : match the consumer attributes:
       - Configuration ':myElements' declares attribute 'color' with value 'blue':
           - Incompatible because this component declares attribute 'artifactType' with value 'jar' and the consumer needed attribute 'artifactType' with value 'dll'
       - Configuration ':myElements' variant secondary declares attribute 'color' with value 'blue':
           - Incompatible because this component declares attribute 'artifactType' with value 'jar' and the consumer needed attribute 'artifactType' with value 'dll'""")

        and: "Helpful resolutions are provided"
        assertSuggestsReviewingAlgorithm()
        assertSuggestsViewingDocs("No matching variant errors are explained in more detail at https://docs.gradle.org/${GradleVersion.current().version}/userguide/variant_model.html#sub:variant-no-match.")
    }

    def "demonstrate ambiguous artifact transforms exception"() {
        ambiguousArtifactTransforms.prepare()

        expect:
        assertResolutionFailsAsExpected(ambiguousArtifactTransforms)

        and: "Has error output"
        failure.assertHasDescription("Could not determine the dependencies of task ':forceResolution'.")
        failure.assertHasCause("Could not resolve all dependencies for configuration ':resolveMe'.")
        assertFullMessageCorrect("""   > Found multiple transforms that can produce a variant of project : with requested attributes:
       - color 'red'
       - shape 'round'
     Found the following transforms:
       - From 'configuration ':roundBlueLiquidElements'':
           - With source attributes:
               - color 'blue'
               - shape 'round'
               - state 'liquid'
           - Candidate transform(s):
               - Transform 'BrokenTransform' producing attributes:
                   - color 'red'
                   - shape 'round'
                   - state 'gas'
               - Transform 'BrokenTransform' producing attributes:
                   - color 'red'
                   - shape 'round'
                   - state 'solid'""")

        and: "Helpful resolutions are provided"
        assertSuggestsReviewingAlgorithm()
        assertSuggestsViewingDocs("Transformation failures are explained in more detail at https://docs.gradle.org/${GradleVersion.current().version}/userguide/variant_model.html#sub:transform-ambiguity.")
    }

    def "demonstrate ambiguous artifact variants exception"() {
        ambiguousArtifactVariants.prepare()

        expect:
        assertResolutionFailsAsExpected(ambiguousArtifactVariants)

        and: "Has error output"
        failure.assertHasDescription("Could not determine the dependencies of task ':forceResolution'.")
        failure.assertHasCause("Could not resolve all dependencies for configuration ':resolveMe'.")
        assertFullMessageCorrect("""   > More than one variant of project : matches the consumer attributes:
       - Configuration ':default' variant v1
       - Configuration ':default' variant v2""")

        and: "Helpful resolutions are provided"
        assertSuggestsReviewingAlgorithm()
        assertSuggestsViewingDocs("Ambiguity errors are explained in more detail at https://docs.gradle.org/${GradleVersion.current().version}/userguide/variant_model.html#sub:variant-ambiguity.")
    }
    // endregion Stage 4 - ArtifactSelectionFailure

    // region dependencyInsight failures
    /**
     * Running the dependencyInsight report can also generate a variant selection failure, but this
     * does <strong>NOT</strong> cause the task to fail.
     */
    def "demonstrate dependencyInsight report no matching capabilities failure"() {
        setupDependencyInsightFailure()

        expect:
        succeeds "dependencyInsight", "--configuration", "compileClasspath", "--dependency", "gson"

        String basicOutput = """   Failures:
      - Could not resolve com.google.code.gson:gson:2.8.5."""
        String fullOutput = """          - Unable to find a variant providing the requested capability 'com.google.code.gson:gson-test-fixtures':
               - Variant 'compile' provides 'com.google.code.gson:gson:2.8.5'
               - Variant 'enforced-platform-compile' provides 'com.google.code.gson:gson-derived-enforced-platform:2.8.5'
               - Variant 'enforced-platform-runtime' provides 'com.google.code.gson:gson-derived-enforced-platform:2.8.5'
               - Variant 'javadoc' provides 'com.google.code.gson:gson:2.8.5'
               - Variant 'platform-compile' provides 'com.google.code.gson:gson-derived-platform:2.8.5'
               - Variant 'platform-runtime' provides 'com.google.code.gson:gson-derived-platform:2.8.5'
               - Variant 'runtime' provides 'com.google.code.gson:gson:2.8.5'
               - Variant 'sources' provides 'com.google.code.gson:gson:2.8.5'"""

        outputContains(basicOutput)
        outputContains(fullOutput)
    }
    // endregion dependencyInsight failures

    // region error showcase
    @spock.lang.Ignore("This test is used to generate a summary of all possible errors, it shouldn't usually be run as part of testing")
    def "generate resolution failure showcase report"() {
        given:
        // Escape to the root of the dependency-management project, to put these reports in build output dir so it isn't auto-deleted when test completes
        File reportFile = testDirectory.parentFile.parentFile.parentFile.parentFile.file("reports/tests/resolution-failure-showcase.txt")

        when:
        generateFailureShowcase(reportFile)

        then:
        assertAllExceptionsInShowcase(reportFile)

        println("Resolution error showcase report available at: ${reportFile.toURI()}")
    }

    private void generateFailureShowcase(TestFile reportFile) {
        StringBuilder reportBuilder = new StringBuilder()
        demonstrations.each {
            buildKotlinFile.text = ""
            it.prepare()

            fails "forceResolution"

            reportBuilder.append("----------------------------------------------------------------------------------------------------\n")
            reportBuilder.append("${it.name}\n")
            reportBuilder.append(result.getError())
            reportBuilder.append("\n")
        }

        reportFile.text = reportBuilder.toString()
    }

    private void assertAllExceptionsInShowcase(TestFile reportFile) {
        String report = reportFile.text
        demonstrations.each {
            report.contains(it.name)
            report.contains("Caused by: " + it.exception.getName())
        }
    }

    @SuppressWarnings('GroovyAccessibility')
    private static class Demonstration {
        private final String name
        private final Class<AbstractResolutionFailureException> exception
        private final Class<? extends ResolutionFailure> failure
        private final Closure setup

        private Demonstration(String name, Class<AbstractResolutionFailureException> exception, Class<? extends ResolutionFailure> failure, Closure setup) {
            this.name = name
            this.exception = exception
            this.failure = failure
            this.setup = setup
        }

        void prepare() {
            setup.call()
        }
    }

    private final Demonstration ambiguousGraphVariantForProjectWithSingleDisambiguatingAttribute = new Demonstration("Ambiguous graph variant (project with single disambiguating attribute)", VariantSelectionByAttributesException.class, AmbiguousVariantsFailure.class, this.&setupAmbiguousGraphVariantFailureForProjectWithSingleDisambiguatingAttribute)
    private final Demonstration ambiguousGraphVariantForProjectWithoutSingleDisambiguatingAttribute = new Demonstration("Ambiguous graph variant (project without single disambiguating attribute)", VariantSelectionByAttributesException.class, AmbiguousVariantsFailure.class, this.&setupAmbiguousGraphVariantFailureForProjectWithoutSingleDisambiguatingAttribute)
    private final Demonstration ambiguousGraphVariantForExternalDep = new Demonstration("Ambiguous graph variant (external)", VariantSelectionByAttributesException.class, AmbiguousVariantsFailure.class, this.&setupAmbiguousGraphVariantFailureForExternalDep)
    private final Demonstration noMatchingGraphVariantsForProject = new Demonstration("No matching graph variants (project dependency)", VariantSelectionByAttributesException.class, NoCompatibleVariantsFailure.class, this.&setupNoMatchingGraphVariantsFailureForProject)
    private final Demonstration noMatchingGraphVariantsForExternalDep = new Demonstration("No matching graph variants (external dependency)", VariantSelectionByAttributesException.class, NoCompatibleVariantsFailure.class, this.&setupNoMatchingGraphVariantsFailureForExternalDep)
    private final Demonstration noGraphVariantsExistForProject = new Demonstration("No variants exist (project dependency)", VariantSelectionByAttributesException.class, NoCompatibleVariantsFailure.class, this.&setupNoGraphVariantsExistFailureForProject)

    private final Demonstration incompatibleRequestedConfiguration = new Demonstration("Incompatible requested configuration", VariantSelectionByNameException.class, ConfigurationNotCompatibleFailure.class, this.&setupConfigurationNotCompatibleFailureForProject)
    private final Demonstration configurationNotFound = new Demonstration("Configuration not found", VariantSelectionByNameException.class, ConfigurationDoesNotExistFailure.class, this.&setupConfigurationNotFound)

    private final Demonstration noMatchingArtifactVariants = new Demonstration("No matching artifact variants", ArtifactSelectionException.class, NoCompatibleArtifactFailure.class, this.&setupNoMatchingArtifactVariantsFailureForProject)
    private final Demonstration ambiguousArtifactTransforms = new Demonstration("Ambiguous artifact transforms", ArtifactSelectionException.class, AmbiguousArtifactTransformsFailure.class, this.&setupAmbiguousArtifactTransformFailureForProject)
    private final Demonstration ambiguousArtifactVariants = new Demonstration("Ambiguous artifact variants", ArtifactSelectionException.class, AmbiguousArtifactsFailure.class, this.&setupAmbiguousArtifactsFailureForProject)

    private final Demonstration incompatibleArtifactVariants = new Demonstration("Incompatible graph variants", GraphValidationException.class, IncompatibleMultipleNodesValidationFailure.class, this.&setupIncompatibleMultipleNodesValidationFailureForProject)

    private final List<Demonstration> demonstrations = [
        ambiguousGraphVariantForProjectWithSingleDisambiguatingAttribute,
        ambiguousGraphVariantForProjectWithoutSingleDisambiguatingAttribute,
        ambiguousGraphVariantForExternalDep,
        noMatchingGraphVariantsForProject,
        noMatchingGraphVariantsForExternalDep,
        noGraphVariantsExistForProject,

        incompatibleRequestedConfiguration,
        configurationNotFound,

        noMatchingArtifactVariants,
        ambiguousArtifactTransforms,
        ambiguousArtifactVariants,

        incompatibleArtifactVariants
    ]
    // endregion error showcase

    // region setup
    private void setupConfigurationNotFound() {
        buildKotlinFile << """
            configurations {
                dependencyScope("myDependencies")

                resolvable("resolveMe") {
                    extendsFrom(configurations.getByName("myDependencies"))
                }
            }

            dependencies {
                add("myDependencies", project(":", "absent"))
            }

            ${forceConsumerResolution()}
        """
    }

    private void setupAmbiguousArtifactsFailureForProject() {
        buildKotlinFile << """
            configurations {
                consumable("default") {
                    outgoing {
                        variants {
                            val v1 by creating { }
                            val v2 by creating { }
                        }
                    }
                }

                dependencyScope("myDependencies")

                resolvable("resolveMe") {
                    extendsFrom(configurations.getByName("myDependencies"))
                }
            }

            dependencies {
                add("myDependencies", project(":"))
            }

            ${forceConsumerResolution()}
        """
    }

    private void setupAmbiguousArtifactTransformFailureForProject() {
        buildKotlinFile <<  """
            val color = Attribute.of("color", String::class.java)
            val shape = Attribute.of("shape", String::class.java)
            val matter = Attribute.of("state", String::class.java)

            configurations {
                consumable("roundBlueLiquidElements") {
                    attributes.attribute(shape, "round")
                    attributes.attribute(color, "blue")
                    attributes.attribute(matter, "liquid")
                }

                dependencyScope("myDependencies")

                resolvable("resolveMe") {
                    extendsFrom(configurations.getByName("myDependencies"))
                    // Initially request only round
                    attributes.attribute(shape, "round")
                }
            }

            abstract class BrokenTransform : TransformAction<TransformParameters.None> {
                override fun transform(outputs: TransformOutputs) {
                    throw AssertionError("Should not actually be selected to run")
                }
            }

            dependencies {
                add("myDependencies", project(":"))

                // Register 2 transforms that both will move blue -> red, but also do
                // something else to another irrelevant attribute in order to make them
                // unique from each other
                registerTransform(BrokenTransform::class.java) {
                    from.attribute(color, "blue")
                    to.attribute(color, "red")
                    from.attribute(matter, "liquid")
                    to.attribute(matter, "solid")
                }
                registerTransform(BrokenTransform::class.java) {
                    from.attribute(color, "blue")
                    to.attribute(color, "red")
                    from.attribute(matter, "liquid")
                    to.attribute(matter, "gas")
                }
            }

            val forceResolution by tasks.registering {
                inputs.files(configurations.getByName("resolveMe").incoming.artifactView {
                    attributes.attribute(color, "red")
                }.artifacts.artifactFiles)

                doLast {
                    inputs.files.files.forEach { println(it) }
                }
            }
        """
    }

    private void setupNoMatchingArtifactVariantsFailureForProject() {
        buildKotlinFile << """
            val artifactType = Attribute.of("artifactType", String::class.java)
            val color = Attribute.of("color", String::class.java)

            configurations {
                consumable("myElements") {
                    attributes.attribute(color, "blue")

                    outgoing {
                        variants {
                            val secondary by creating {
                                // Without artifacts on the variant, we would get a AmbiguousArtifactVariantsException - need a mismatch with the derived artifact type of "jar"
                                artifact(file("secondary.jar"))
                            }
                        }

                        artifacts {
                            artifact(file("implicit.jar"))
                        }
                    }
                }
            }

            configurations {
                dependencyScope("myDependencies")

                resolvable("resolveMe") {
                    extendsFrom(configurations.getByName("myDependencies"))

                    // We need to match the "myElements" configuration, then the AV will look at its variants
                    attributes.attribute(color, "blue")
                    // Without requesting this special attribute that mismatches the derived value of "jar", we would get a successful result that pulls implicit.jar
                    attributes.attribute(artifactType, "dll")
                }
            }

            dependencies {
                add("myDependencies", project(":"))
            }

            ${forceConsumerResolution()}
        """
    }

    private void setupAmbiguousGraphVariantFailureForProjectWithSingleDisambiguatingAttribute() {
        buildKotlinFile <<  """
            val color = Attribute.of("color", String::class.java)
            val shape = Attribute.of("shape", String::class.java)

            configurations {
                consumable("blueRoundElements") {
                    attributes.attribute(color, "blue")
                    attributes.attribute(shape, "round")
                }
                consumable("blueSquareElements") {
                    attributes.attribute(color, "blue")
                    attributes.attribute(shape, "square")
                }
                consumable("blueTriangleElements") {
                    attributes.attribute(color, "blue")
                    attributes.attribute(shape, "triangle")
                }

                dependencyScope("blueFilesDependencies")

                resolvable("resolveMe") {
                    extendsFrom(configurations.getByName("blueFilesDependencies"))
                    attributes.attribute(color, "blue")
                }
            }

            dependencies {
                add("blueFilesDependencies", project(":"))
            }

            ${forceConsumerResolution()}
        """
    }

    private void setupAmbiguousGraphVariantFailureForProjectWithoutSingleDisambiguatingAttribute() {
        buildKotlinFile << """
            val color = Attribute.of("color", String::class.java)
            val shape = Attribute.of("shape", String::class.java)
            val opacity = Attribute.of("opacity", String::class.java)

            configurations {
                consumable("blueRoundTransparentElements") {
                    attributes.attribute(color, "blue")
                    attributes.attribute(shape, "round")
                    attributes.attribute(opacity, "transparent")
                }
                consumable("blueSquareOpaqueElements") {
                    attributes.attribute(color, "blue")
                    attributes.attribute(shape, "square")
                    attributes.attribute(opacity, "opaque")
                }
                consumable("blueSquareTransparentElements") {
                    attributes.attribute(color, "blue")
                    attributes.attribute(shape, "square")
                    attributes.attribute(opacity, "transparent")
                }

                dependencyScope("blueFilesDependencies")

                resolvable("resolveMe") {
                    extendsFrom(configurations.getByName("blueFilesDependencies"))
                    attributes.attribute(color, "blue")
                }
            }

            dependencies {
                add("blueFilesDependencies", project(":"))
            }

            ${forceConsumerResolution()}
        """
    }

    private void setupAmbiguousGraphVariantFailureForExternalDep() {
        buildKotlinFile <<  """
            ${mavenCentralRepository(GradleDsl.KOTLIN)}

            configurations {
                dependencyScope("myLibs")

                resolvable("resolveMe") {
                    extendsFrom(configurations.getByName("myLibs"))
                    attributes {
                        attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category::class.java, Category.DOCUMENTATION))
                    }
                }
            }

            dependencies {
                add("myLibs", "com.squareup.okhttp3:okhttp:4.4.0")
            }

            ${forceConsumerResolution()}
        """
    }

    private void setupNoMatchingGraphVariantsFailureForProject() {
        buildKotlinFile << """
            plugins {
                id("base")
            }

            val color = Attribute.of("color", String::class.java)

            configurations {
                val default by getting {
                    attributes.attribute(color, "blue")
                }

                dependencyScope("defaultDependencies")

                resolvable("resolveMe") {
                    extendsFrom(configurations.getByName("defaultDependencies"))
                    attributes.attribute(color, "green")
                }
            }

            dependencies {
                add("defaultDependencies", project(":"))
            }

            ${forceConsumerResolution()}
        """
    }

    private void setupNoMatchingGraphVariantsFailureForExternalDep() {
        buildKotlinFile <<  """
            ${mavenCentralRepository(GradleDsl.KOTLIN)}

            configurations {
                dependencyScope("myLibs")

                resolvable("resolveMe") {
                    extendsFrom(configurations.getByName("myLibs"))
                    attributes {
                        attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category::class.java, "non-existent-format"))
                    }
                }
            }

            dependencies {
                add("myLibs", "com.squareup.okhttp3:okhttp:4.4.0")
            }

            ${forceConsumerResolution()}
        """
    }

    private void setupNoGraphVariantsExistFailureForProject() {
        settingsKotlinFile << """
            rootProject.name = "example"
            include("producer")
        """

        buildKotlinFile << """
            plugins {
                id("base")
            }

            val color = Attribute.of("color", String::class.java)

            configurations {
                dependencyScope("defaultDependencies")

                resolvable("resolveMe") {
                    extendsFrom(configurations.getByName("defaultDependencies"))
                    attributes.attribute(color, "green")
                }
            }

            dependencies {
                add("defaultDependencies", project(":producer"))
            }

            ${forceConsumerResolution()}
        """

        file("producer/build.gradle.kts").touch()
    }

    private void setupConfigurationNotCompatibleFailureForProject() {
        buildKotlinFile <<  """
            plugins {
                id("base")
            }

            val color = Attribute.of("color", String::class.java)

            configurations {
                val mismatch by configurations.creating {
                    attributes.attribute(color, "blue")
                }

                dependencyScope("defaultDependencies")

                resolvable("resolveMe") {
                    extendsFrom(configurations.getByName("defaultDependencies"))
                    attributes.attribute(color, "green")
                }
            }

            dependencies {
                add("defaultDependencies", project(":", "mismatch"))
            }

            ${forceConsumerResolution()}
        """
    }

    private void setupIncompatibleMultipleNodesValidationFailureForProject() {
        buildKotlinFile <<  """
            group = "org.example"
            version = "1.0"

            val color = Attribute.of("color", String::class.java)

            // TODO: Can't use dependencyScope here yet, as it doesn't support capabilities
            val incompatible: Configuration by configurations.creating {
                isCanBeDeclared = true
                isCanBeConsumed = false
                isCanBeResolved = false
            }

            configurations {
                consumable("blueElementsCapability1") {
                    attributes.attribute(color, "blue")
                    outgoing {
                        capability("org.example:cap1:1.0")
                    }
                }
                consumable("blueElementsCapability2") {
                    attributes.attribute(color, "blue")
                    outgoing {
                        capability("org.example:cap2:1.0")
                    }
                }

                consumable("greenElementsCapability1") {
                    attributes.attribute(color, "green")
                    outgoing {
                        capability("org.example:cap1:1.0")
                    }
                }
                consumable("greenElementsCapability2") {
                    attributes.attribute(color, "green")
                    outgoing {
                        capability("org.example:cap2:1.0")
                    }
                }

                resolvable("resolveMe") {
                    extendsFrom(incompatible)
                }
            }

            dependencies {
                add("incompatible", project(":")) {
                    attributes {
                        attribute(color, "blue")
                    }
                    capabilities {
                        requireCapability("org.example:cap1:1.0")
                    }
                }
                add("incompatible", project(":")) {
                    attributes {
                        attribute(color, "green")
                    }
                    capabilities {
                        requireCapability("org.example:cap2:1.0")
                    }
                }
            }

            ${forceConsumerResolution()}
        """
    }

    private void setupDependencyInsightFailure() {
        buildKotlinFile <<  """
            plugins {
                `java-library`
                `java-test-fixtures`
            }

            ${mavenCentralRepository(GradleDsl.KOTLIN)}

            dependencies {
                // Adds a dependency on the test fixtures of Gson, however this
                // project doesn't publish such a thing
                implementation(testFixtures("com.google.code.gson:gson:2.8.5"))
            }
        """

    }

    private String forceConsumerResolution() {
        return """
            abstract class ForceResolution : DefaultTask() {
                @get:InputFiles
                abstract val resolvedFiles: ConfigurableFileCollection
            }

            val forceResolution by tasks.registering(ForceResolution::class) {
                resolvedFiles.from(configurations.getByName("resolveMe"))
                doLast {
                    resolvedFiles.forEach { println(it) }
                }
            }
        """
    }
    // endregion setup

    private void assertFullMessageCorrect(String identifyingFragment) {
        failure.assertHasErrorOutput(identifyingFragment)
    }

    private void assertSuggestsReviewingAlgorithm() {
        assertSuggestsViewingDocs("Review the variant matching algorithm at https://docs.gradle.org/${GradleVersion.current().version}/userguide/variant_attributes.html#sec:abm_algorithm.")
    }

    private void assertSuggestsViewingDocs(String resolution) {
        failure.assertHasResolution(resolution)
    }

    private void assertResolutionFailsAsExpected(Demonstration demonstration) {
        fails "forceResolution", "--stacktrace", "--info"

        outputContains("Variant Selection Exception: " + demonstration.exception.getName() + " caused by Resolution Failure: " + demonstration.failure.getName())
        failure.assertHasErrorOutput("Caused by: " + demonstration.exception.getName())
    }
}
