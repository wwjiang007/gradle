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
            def consumerBuildFile = file('consumer/build.gradle')

            producerBuildFile << '''
                plugins {
                    id 'java'
                }

                java {
                    withJavadocJar()
                }

                def otherAttribute = Attribute.of('other', String)

                //dependencies.attributesSchema {
                //    attribute(otherAttribute)
                //}

                configurations {
                    other {
                        canBeResolved = false
                        attributes {
                            attribute(otherAttribute, 'foobar')
                        }

                        outgoing {
                            //capability('org.test:producer-other:1.0')
                            artifact(layout.buildDirectory.file('other.txt'))
                        }
                    }
                }
            '''

            consumerBuildFile << '''
                configurations {
                    producerArtifacts {
                        canBeConsumed = false
                        attributes {
                            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, Category.LIBRARY))
                            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage, Usage.JAVA_RUNTIME))
                        }
                    }
                }

                def otherAttribute = Attribute.of('other', String)

                //dependencies.attributesSchema {
                //    attribute(otherAttribute)
                //}

                dependencies {
                    producerArtifacts project(':producer')
                }

                tasks.register('resolve') {
                    dependsOn configurations.producerArtifacts
                    doLast {
                        logger.lifecycle 'Found files: {}', configurations.producerArtifacts.files*.name
                    }
                }

                tasks.register('resolveOther') {
                    dependsOn configurations.producerArtifacts
                    doLast {
                        def view = configurations.producerArtifacts.incoming.artifactView {
                            lenient = true

                            attributes {
                                attribute(otherAttribute, 'foobar')
                            }
                        }
                        logger.lifecycle 'Found files: {}', view.files*.name
                        //logger.lifecycle 'Found files: {}', view.artifacts.artifactFiles.files*.name
                    }
                }

                tasks.register('resolveDocumentation') {
                    dependsOn configurations.producerArtifacts
                    doLast {
                        def view = configurations.producerArtifacts.incoming.artifactView {
                            lenient = true

                            attributes {
                                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, Category.DOCUMENTATION))
                            }
                        }
                        logger.lifecycle 'Found files: {}', view.files*.name
                        //logger.lifecycle 'Found files: {}', view.artifacts.artifactFiles.files*.name
                    }
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
Variant other
--------------------------------------------------
Capabilities
    - org.test:producer:1.0 (default capability)
Attributes
    - other = foobar

Artifacts
    - build/other.txt (artifactType = txt)'''
    }

    def 'consumer resolves runtimeElements variant of producer'() {
        expect:
        succeeds(':consumer:resolve')
        outputContains('Found files: [producer-1.0.jar]')
    }

    def 'consumer resolves other variant of producer'() {
        expect:
        succeeds(':consumer:resolveOther')
        succeeds(':consumer:dependencyInsight', '--configuration', 'producerArtifacts', '--dependency', 'producer')
        outputContains('Found files: [other.txt]')
    }

    def 'consumer resolves javadocElements variant of producer'() {
        expect:
        succeeds(':consumer:resolveDocumentation')
        //succeeds(':consumer:dependencyInsight', '--configuration', 'producerArtifacts', '--dependency', 'producer')
        outputContains('Found files: [producer-1.0-javadoc.jar]')
    }
}
