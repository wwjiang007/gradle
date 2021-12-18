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

class DerivedVariantsResolutionIntegrationTest extends AbstractHttpDependencyResolutionTest {
    MavenHttpModule direct
    MavenHttpModule transitive

    def setup() {
        buildFile << """
            plugins {
                id 'java'
            }

            repositories {
                maven { url '$mavenHttpRepo.uri' }
            }

            dependencies {
                implementation 'test:direct:1.0'
            }

            abstract class Resolve extends DefaultTask {
                @InputFiles
                abstract ConfigurableFileCollection getArtifacts()

                @Internal
                List<String> expectations = []

                @TaskAction
                void assertThat() {
                    assert artifacts.files*.name == expectations
                }
            }

            task resolve(type: Resolve) {
                def artifactView = configurations.runtimeClasspath.incoming.artifactView {
                    lenient = true
                    withVariantReselection()
                    attributes {
                        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage, Usage.JAVA_RUNTIME))
                        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, Category.DOCUMENTATION))
                        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling, Bundling.EXTERNAL))
                        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType, DocsType.SOURCES))
                    }
                }
                artifacts.from(artifactView.getFiles())
            }
        """
        transitive = mavenHttpRepo.module("test", "transitive", "1.0")
        direct = mavenHttpRepo.module("test", "direct", "1.0")
        direct.dependsOn(transitive)

        //         transitive.withSourceAndJavadoc()
        //         transitive.withModuleMetadata()
    }

    // region With Gradle Module Metadata
    def "direct has GMM and no sources jar"() {
        transitive.withModuleMetadata()
        transitive.publish()
        direct.withModuleMetadata()
        direct.publish()

        buildFile << """
            resolve {
                expectations = []
            }
        """
        expect:
        direct.pom.expectGet()
        direct.moduleMetadata.expectGet()
        transitive.pom.expectGet()
        transitive.moduleMetadata.expectGet()
        direct.artifact(classifier: "sources").expectGetMissing()
        transitive.artifact(classifier: "sources").expectGetMissing()

        succeeds( "resolve")
    }

    def "direct has GMM and has sources jar"() {
        transitive.adhocVariants().variant("jar", [
            "org.gradle.category": "library",
            "org.gradle.dependency.bundling": "external",
            "org.gradle.usage": "java-runtime"
        ]) {
            artifact("transitive-1.0.jar")
        }
        .variant("sources", [
                "org.gradle.category": "documentation",
                "org.gradle.dependency.bundling": "external",
                "org.gradle.docstype": "sources",
                "org.gradle.usage": "java-runtime"
        ]) {
            artifact("transitive-1.0-sources.jar")
        }
        transitive.withModuleMetadata()
        transitive.publish()

        direct.adhocVariants().variant("jar", [
            "org.gradle.category": "library",
            "org.gradle.dependency.bundling": "external",
            "org.gradle.usage": "java-runtime"
        ]) {
            artifact("direct-1.0.jar")
        }
        .variant("sources", [
                "org.gradle.category": "documentation",
                "org.gradle.dependency.bundling": "external",
                "org.gradle.docstype": "sources",
                "org.gradle.usage": "java-runtime"
        ]) {
            artifact("direct-1.0-sources.jar")
        }
        direct.withModuleMetadata()
        direct.publish()

        buildFile << """
            resolve {
                expectations = ['direct-1.0-sources.jar', 'transitive-1.0-sources.jar']
            }
        """
        expect:
        direct.pom.expectGet()
        direct.moduleMetadata.expectGet()
        transitive.pom.expectGet()
        transitive.moduleMetadata.expectGet()
        direct.artifact(classifier: "sources").expectGet()
        transitive.artifact(classifier: "sources").expectGet()

        succeeds( "resolve")
    }

    def "direct has GMM and no sources jar and transitive has sources jar"() {
        transitive.adhocVariants().variant("sources", [
            "org.gradle.category": "documentation",
            "org.gradle.dependency.bundling": "external",
            "org.gradle.docstype": "sources",
            "org.gradle.usage": "java-runtime"
        ]) {
            artifact("transitive-1.0-sources.jar")
        }
        transitive.withModuleMetadata()
        transitive.publish()

        direct.withModuleMetadata()
        direct.publish()

        buildFile << """
            resolve {
                expectations = []
            }
        """
        expect:
        direct.pom.expectGet()
        direct.moduleMetadata.expectGet()
        transitive.pom.expectGet()
        transitive.moduleMetadata.expectGet()
        transitive.artifact(classifier: "sources").expectGet()

        succeeds( "resolve")
    }
    // endregion

    // region Without Gradle Module Metadata
    def "direct has no GMM and no sources jar"() {
        transitive.publish()
        direct.publish()

        buildFile << """
            resolve {
                expectations = []
            }
        """
        expect:
        direct.pom.expectGet()
        transitive.pom.expectGet()
        direct.artifact(classifier: "sources").expectGetMissing()
        transitive.artifact(classifier: "sources").expectGetMissing()

        succeeds( "resolve")
    }

    def "direct has no GMM and has sources jar"() {
        direct.withSourceAndJavadoc()
        transitive.withSourceAndJavadoc()

        transitive.publish()
        direct.publish()

        buildFile << """
            resolve {
                expectations = ["direct-1.0-sources.jar", "transitive-1.0-sources.jar"]
            }
        """
        expect:
        direct.pom.expectGet()
        transitive.pom.expectGet()
        direct.artifact(classifier: "sources").expectGet()
        transitive.artifact(classifier: "sources").expectGet()

        succeeds("resolve")
    }

    def "direct has no GMM and no sources jar and transitive has sources jar"() {
        transitive.withSourceAndJavadoc()
        transitive.publish()
        direct.publish()

        buildFile << """
            resolve {
                expectations = []
            }
        """
        expect:
        direct.pom.expectGet()
        transitive.pom.expectGet()
        direct.artifact(classifier: "sources").expectGetMissing()
        transitive.artifact(classifier: "sources").expectGet()

        succeeds( "resolve")
    }
    // endregion
}
