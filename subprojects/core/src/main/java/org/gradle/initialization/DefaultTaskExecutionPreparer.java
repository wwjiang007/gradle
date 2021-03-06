/*
 * Copyright 2019 the original author or authors.
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

package org.gradle.initialization;

import org.gradle.api.internal.GradleInternal;
import org.gradle.composite.internal.IncludedBuildControllers;
import org.gradle.execution.BuildConfigurationActionExecuter;
import org.gradle.execution.taskgraph.TaskExecutionGraphInternal;
import org.gradle.internal.buildtree.BuildModelParameters;
import org.gradle.internal.operations.BuildOperationExecutor;

public class DefaultTaskExecutionPreparer implements TaskExecutionPreparer {
    private final BuildOperationExecutor buildOperationExecutor;
    private final BuildConfigurationActionExecuter buildConfigurationActionExecuter;
    private final IncludedBuildControllers includedBuildControllers;
    private final BuildModelParameters buildModelParameters;

    public DefaultTaskExecutionPreparer(BuildConfigurationActionExecuter buildConfigurationActionExecuter,
                                        IncludedBuildControllers includedBuildControllers,
                                        BuildOperationExecutor buildOperationExecutor,
                                        BuildModelParameters buildModelParameters) {
        this.buildConfigurationActionExecuter = buildConfigurationActionExecuter;
        this.includedBuildControllers = includedBuildControllers;
        this.buildOperationExecutor = buildOperationExecutor;
        this.buildModelParameters = buildModelParameters;
    }

    @Override
    public void prepareForTaskExecution(GradleInternal gradle) {
        buildConfigurationActionExecuter.select(gradle);

        TaskExecutionGraphInternal taskGraph = gradle.getTaskGraph();
        taskGraph.populate();

        if (gradle.isRootBuild()) {
            // Force the population of other task graphs to happen as part of the build operation for populating
            // the root build task graph. Should get rid of this nesting instead
            includedBuildControllers.populateTaskGraphs();
        }

        if (buildModelParameters.isConfigureOnDemand() && gradle.isRootBuild()) {
            new ProjectsEvaluatedNotifier(buildOperationExecutor).notify(gradle);
        }
    }
}
