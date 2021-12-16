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

package org.gradle.integtests.resolve.derived

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class MultiProjectVariantResolutionIntegrationTest extends AbstractIntegrationSpec {

    def setup() {
        multiProjectBuild('root', ['producer', 'consumer']) {
            buildFile << '''

            '''

            def producerBuildFile = file('producer/build.gradle')
            file("producer/jar.txt").text = "jar file"
            file("producer/javadoc.txt").text = "javadoc file"
            file("producer/other.txt").text = "other file"

            producerBuildFile << '''
                configurations {
                    jarElements {
                        canBeResolved = false
                        canBeConsumed = true
                        attributes {
                            attribute(Attribute.of('shared', String), 'shared-value')
                            attribute(Attribute.of('unique', String), 'jar-value')
                        }

                        outgoing {
                            artifact(layout.projectDirectory.file('jar.txt'))
                        }
                    }
                    javadocElements {
                        canBeResolved = false
                        canBeConsumed = true
                        attributes {
                            attribute(Attribute.of('shared', String), 'shared-value')
                            attribute(Attribute.of('unique', String), 'javadoc-value')
                        }

                        outgoing {
                            artifact(layout.projectDirectory.file('javadoc.txt'))
                        }
                    }
                    otherElements {
                        canBeResolved = false
                        canBeConsumed = true
                        attributes {
                            attribute(Attribute.of('other', String), 'foobar')
                        }

                        outgoing {
                            artifact(layout.projectDirectory.file('other.txt'))
                        }
                    }
                }
            '''

            def consumerBuildFile = file('consumer/build.gradle')
            consumerBuildFile << '''
                configurations {
                    producerArtifacts {
                        canBeConsumed = false
                        canBeResolved = true

                        attributes {
                            attribute(Attribute.of('shared', String), 'shared-value')
                            attribute(Attribute.of('unique', String), 'jar-value')
                        }
                    }
                }

                dependencies {
                    producerArtifacts project(':producer')
                }

                abstract class Resolve extends DefaultTask {
                    @InputFiles
                    abstract ConfigurableFileCollection getArtifacts()

                    @Internal
                    List<String> expectations = []

                    @TaskAction
                    void assertThat() {
                        logger.lifecycle 'Found files: {}', artifacts.files*.name
                        assert artifacts.files*.name == expectations
                    }
                }

                tasks.register('resolve', Resolve) {
                    artifacts.from(configurations.producerArtifacts)
                    expectations = [ 'jar.txt' ]
                }

                tasks.register('resolveJavadoc', Resolve) {
                    artifacts.from(configurations.producerArtifacts.incoming.variantView {
                        attributes {
                            attribute(Attribute.of('unique', String), 'javadoc-value')
                        }
                    }.files)
                    expectations = [ 'javadoc.txt' ]
                }

                tasks.register('resolveOther', Resolve) {
                    artifacts.from(configurations.producerArtifacts.incoming.variantView {
                        attributes {
                            attribute(Attribute.of('other', String), 'foobar')
                        }
                    }.files)
                    expectations = [ 'other.txt' ]
                }
            '''
        }
    }

    def 'producer has expected outgoingVariants'() {
        when:
        succeeds(':producer:outgoingVariants')

        then:
        outputContains '''
--------------------------------------------------
Variant jarElements
--------------------------------------------------
Capabilities
    - org.test:producer:1.0 (default capability)
Attributes
    - shared = shared-value
    - unique = jar-value

Artifacts
    - jar.txt

--------------------------------------------------
Variant javadocElements
--------------------------------------------------
Capabilities
    - org.test:producer:1.0 (default capability)
Attributes
    - shared = shared-value
    - unique = javadoc-value

Artifacts
    - javadoc.txt

--------------------------------------------------
Variant otherElements
--------------------------------------------------
Capabilities
    - org.test:producer:1.0 (default capability)
Attributes
    - other = foobar

Artifacts
    - other.txt
'''
    }

    def 'consumer resolves runtimeElements variant of producer'() {
        expect:
        succeeds(':consumer:resolve')
        succeeds(':consumer:dependencyInsight', '--configuration', 'producerArtifacts', '--dependency', 'producer')
    }

    def 'consumer resolves other variant of producer'() {
        expect:
        succeeds(':consumer:resolveOther')
    }

    def 'consumer resolves javadocElements variant of producer'() {
        expect:
        succeeds(':consumer:resolveJavadoc')
    }
}
