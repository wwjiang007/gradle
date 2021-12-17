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

import org.gradle.integtests.fixtures.AbstractHttpDependencyResolutionTest
import org.gradle.test.fixtures.server.http.MavenHttpModule

class PublishedVariantResolutionIntegrationTest extends AbstractHttpDependencyResolutionTest {
    MavenHttpModule producer
    MavenHttpModule direct
    MavenHttpModule transitive

    def setup() {
        transitive = mavenHttpRepo.module("test", "transitive", "1.0")
        defineVariants(transitive)
        transitive.publish()

        direct = mavenHttpRepo.module("test", "direct", "1.0")
        defineVariants(direct)
        direct.dependsOn(transitive)
        direct.publish()

        producer = mavenHttpRepo.module("test", "producer", "1.0")

        buildFile << """

            repositories {
                maven { url '$mavenHttpRepo.uri' }
            }
            
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
                    producerArtifacts 'test:producer:1.0'
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
                }

                tasks.register('resolveJavadoc', Resolve) {
                    artifacts.from(configurations.producerArtifacts.incoming.artifactView {
                        attributes {
                            // enableNewBehavior()
                            attribute(Attribute.of('another', String), 'javadoc-value')
                        }
                    }.files)
                }
                
                tasks.register('resolveOther', Resolve) {
                    artifacts.from(configurations.producerArtifacts.incoming.artifactView {
                        attributes {
                            // enableNewBehavior()
                            attribute(Attribute.of('other', String), 'foobar')
                        }
                    }.files)
                }
            """
        producer.allowAll()
        transitive.allowAll()
        direct.allowAll()
    }

    private void defineVariants(MavenHttpModule module) {
        module.adhocVariants().
                variant("jarElements", [shared: 'shared-value', unique: 'jar-value']) {
                    artifact("${module.artifactId}-jar.txt")
                }.
                variant("javadocElements", [shared: 'shared-value', unique: 'jar-value', another: 'javadoc-value']) {
                    artifact("${module.artifactId}-javadoc.txt")
                }.
                variant("otherElements", [other: 'foobar']) {
                    artifact("${module.artifactId}-other.txt")
                }.
                withModuleMetadata()
    }

    def 'consumer resolves jar variant of producer'() {
        defineVariants(producer)
        producer.publish()

        buildFile << '''
            resolve {
                expectations = [ 'producer-jar.txt' ]
            }
        '''
        expect:
        succeeds(':resolve')
    }

    def 'consumer resolves javadoc variant of producer'() {
        defineVariants(producer)
        producer.publish()
        buildFile << '''
            resolveJavadoc {
                expectations = [ 'producer-javadoc.txt' ]
            }
        '''
        expect:
        succeeds(':resolveJavadoc')
    }

    def 'consumer resolves other variant of producer'() {
        defineVariants(producer)
        producer.publish()
        buildFile << '''
            resolveOther {
                expectations = [ 'producer-other.txt' ]
            }
        '''
        expect:
        succeeds(':resolveOther')
    }

    def 'consumer resolves jar variant of producer with dependencies'() {
        defineVariants(producer)
        producer.withVariant("jarElements") {
            dependsOn("test:direct:1.0")
        }
        producer.publish()

        buildFile << '''
            resolve {
                expectations = ['producer-jar.txt', 'direct-jar.txt', 'transitive-jar.txt']
            }
        '''
        expect:
        succeeds(':resolve')
    }

    def 'consumer resolves javadoc variant of producer with dependencies on jarElements'() {
        defineVariants(producer)
        producer.withVariant("jarElements") {
            dependsOn("test:direct:1.0")
        }
        producer.publish()
        buildFile << '''
            resolveJavadoc {
                expectations = ['producer-javadoc.txt', 'direct-javadoc.txt', 'transitive-javadoc.txt']
            }
        '''
        expect:
        succeeds(':resolveJavadoc')
    }

    def 'consumer resolves other variant of producer with dependencies on jarElements'() {
        defineVariants(producer)
        producer.withVariant("jarElements") {
            dependsOn("test:direct:1.0")
        }
        producer.publish()
        buildFile << '''
            resolveOther {
                expectations = ['producer-other.txt', 'direct-other.txt', 'transitive-other.txt']
            }
        '''
        expect:
        succeeds(':resolveOther')
    }

    def 'consumer resolves other variant of producer with dependencies on otherElements'() {
        defineVariants(producer)
        producer.withVariant("otherElements") {
            dependsOn("test:direct:1.0")
        }
        producer.publish()
        buildFile << '''
            resolveOther {
                expectations = ['producer-other.txt']
            }
        '''
        expect:
        succeeds(':resolveOther')
    }
}
