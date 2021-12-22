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

package org.gradle.testing.jacoco.plugins;

import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer;
import org.gradle.api.Incubating;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.TestSuiteType;
import org.gradle.api.attributes.VerificationType;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.api.plugins.jvm.internal.JvmEcosystemUtilities;
import org.gradle.api.reporting.ReportingExtension;
import org.gradle.internal.jacoco.DefaultJacocoCoverageReport;
import org.gradle.testing.base.TestSuite;
import org.gradle.testing.base.TestingExtension;
import org.gradle.testing.jacoco.tasks.JacocoReport;

import javax.inject.Inject;

/**
 * Adds configurations to for resolving variants containing JaCoCo code coverage results, which may span multiple subprojects.  Reacts to the presence of the jvm-test-suite plugin and creates
 * tasks to collect code coverage results for each named test-suite.
 *
 * @see <a href="https://docs.gradle.org/current/userguide/jacoco_report_aggregation_plugin.html">JaCoCo Report Aggregation Plugin reference</a>
 * @since 7.4
 */
@Incubating
public abstract class JacocoReportAggregationPlugin implements Plugin<Project> {

    public static final String JACOCO_AGGREGATION_CONFIGURATION_NAME = "jacocoAggregation";

    @Inject
    protected abstract JvmEcosystemUtilities getJvmEcosystemUtilities();

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("org.gradle.reporting-base");
        project.getPluginManager().apply("jacoco");

        Configuration jacocoAggregation = project.getConfigurations().create(JACOCO_AGGREGATION_CONFIGURATION_NAME);
        jacocoAggregation.setDescription("Collects project dependencies for purposes of JaCoCo coverage report aggregation");
        jacocoAggregation.setVisible(false);
        jacocoAggregation.setCanBeConsumed(false);
        jacocoAggregation.setCanBeResolved(true);
        getJvmEcosystemUtilities().configureAsRuntimeClasspath(jacocoAggregation);

        ObjectFactory objects = project.getObjects();
        ArtifactView sourceDirectories = jacocoAggregation.getIncoming().artifactView(view -> {
            view.componentFilter(id -> id instanceof ProjectComponentIdentifier);
            view.lenient(true);
            view.withVariantReselection();
            view.attributes(attributes -> {
                attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.class, Bundling.EXTERNAL));
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.VERIFICATION));
                attributes.attribute(VerificationType.VERIFICATION_TYPE_ATTRIBUTE, objects.named(VerificationType.class, VerificationType.MAIN_SOURCES));
            });
        });

        ArtifactView classDirectories = jacocoAggregation.getIncoming().artifactView(view -> {
            view.componentFilter(id -> id instanceof ProjectComponentIdentifier);
        });

        ReportingExtension reporting = project.getExtensions().getByType(ReportingExtension.class);
        reporting.getReports().registerBinding(JacocoCoverageReport.class, DefaultJacocoCoverageReport.class);

        // iterate and configure each user-specified report, creating a <reportName>ExecutionData configuration for each
        reporting.getReports().withType(JacocoCoverageReport.class).configureEach(report -> {
            report.getReportTask().configure(task -> {
                ArtifactView executionData = jacocoAggregation.getIncoming().artifactView(view -> {
                    view.componentFilter(id -> id instanceof ProjectComponentIdentifier);
                    view.lenient(true);
                    view.withVariantReselection();
                    view.attributes(attributes -> {
                        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.VERIFICATION));
                        attributes.attributeProvider(TestSuiteType.TEST_SUITE_TYPE_ATTRIBUTE, report.getTestType().map(tt -> objects.named(TestSuiteType.class, "test")));
                        attributes.attribute(VerificationType.VERIFICATION_TYPE_ATTRIBUTE, objects.named(VerificationType.class, VerificationType.JACOCO_RESULTS));
                    });
                });

                configureReportTaskInputs(task, classDirectories, sourceDirectories, executionData);
            });
        });

        // convention for synthesizing reports based on existing test suites in "this" project
        project.getPlugins().withId("jvm-test-suite", plugin -> {
            // Depend on this project for aggregation
            project.getDependencies().add(JACOCO_AGGREGATION_CONFIGURATION_NAME, project);

            TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);
            ExtensiblePolymorphicDomainObjectContainer<TestSuite> testSuites = testing.getSuites();
            testSuites.withType(JvmTestSuite.class).configureEach(testSuite -> {
                reporting.getReports().create(testSuite.getName() + "CodeCoverageReport", JacocoCoverageReport.class, report -> {
                    report.getTestType().convention(testSuite.getTestType());
                });
            });
        });
    }

    private void configureReportTaskInputs(JacocoReport task, ArtifactView classDirectories, ArtifactView sourceDirectories, ArtifactView executionData) {
        task.getExecutionData().from(executionData.getFiles());
        task.getClassDirectories().from(classDirectories.getFiles());
        task.getSourceDirectories().from(sourceDirectories.getFiles());
    }
}
