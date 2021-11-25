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

package org.gradle.configurationcache

import org.gradle.api.Task
import org.gradle.api.execution.TaskActionListener
import org.gradle.internal.operations.BuildOperationExecutor
import org.gradle.internal.operations.BuildOperationRef
import org.gradle.internal.service.scopes.EventScope
import org.gradle.internal.service.scopes.Scopes

@EventScope(Scopes.BuildTree::class)
class TaskExecutionTracker(
    private val buildOperationExecutor: BuildOperationExecutor
) : TaskActionListener {
    private
    val currentThreadExecutesTask = ThreadLocal.withInitial { false }

    private val _currentRunningTaskOperations = HashSet<BuildOperationRef>()
    val currentRunningTaskOperations: Set<BuildOperationRef> = _currentRunningTaskOperations

    override fun beforeActions(task: Task) {
        currentThreadExecutesTask.set(true)
        _currentRunningTaskOperations.add(buildOperationExecutor.currentOperation)
    }

    override fun afterActions(task: Task) {
        currentThreadExecutesTask.set(false)
        _currentRunningTaskOperations.remove(buildOperationExecutor.currentOperation)
    }

    fun isCurrentThreadExecutingTask(): Boolean = currentThreadExecutesTask.get()
}
