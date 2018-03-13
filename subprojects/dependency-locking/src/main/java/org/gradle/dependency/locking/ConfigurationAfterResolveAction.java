/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.dependency.locking;

import org.gradle.api.Action;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.dependency.locking.DependencyLockingDataExchanger.LockfileHandling;
import org.gradle.dependency.locking.exception.LockOutOfDateException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.gradle.dependency.locking.exception.LockOutOfDateException.createLockOutOfDateException;

class ConfigurationAfterResolveAction implements Action<ResolvableDependencies> {

    private static final Logger LOGGER = Logging.getLogger(DependencyLockingPlugin.class);

    private final LockfileReader lockfileReader;
    private final DependencyLockingDataExchanger dataExchanger;

    public ConfigurationAfterResolveAction(LockfileReader lockfileReader, DependencyLockingDataExchanger dataExchanger) {
        this.lockfileReader = lockfileReader;
        this.dataExchanger = dataExchanger;
    }

    @Override
    public void execute(ResolvableDependencies resolvableDependencies) {
        String configurationName = resolvableDependencies.getName();
        LOGGER.warn("Post resolve hook for {}", configurationName);
        Map<String, String> modules = getMapOfResolvedDependencies(resolvableDependencies);
        LockfileHandling lockFileHandling = dataExchanger.getLockFileHandling();
        LockValidationResult validationResult = validateLockAligned(lockFileHandling, modules, lockfileReader.readLockFile(configurationName));

        switch(validationResult.state) {
            case INVALID:
                throw createLockOutOfDateException(validationResult.errors);
            case VALID_APPENDED:
                StringBuilder builder = new StringBuilder("Dependency lock found new modules:\n");
                for (String extraModule : validationResult.extraModules) {
                    builder.append("\t").append(extraModule).append("\n");
                }
                builder.append("\tLock file has been updated with these entries.");
                LOGGER.lifecycle(builder.toString());
        }

        dataExchanger.configurationResolved(configurationName, modules, validationResult.state);
    }

    private LockValidationResult validateLockAligned(LockfileHandling lockFileHandling, Map<String, String> modules, List<String> lines) {
        LockValidationResult.LockValidationState state = LockValidationResult.LockValidationState.VALID;
        if (lines == null) {
            state = LockValidationResult.LockValidationState.NO_LOCK;
        }
        List<String> errors = new ArrayList<String>();
        Set<String> extraModules = new HashSet<String>(modules.keySet());
        if (state != LockValidationResult.LockValidationState.NO_LOCK && lockFileHandling != LockfileHandling.UPDATE_ALL) {
            if (modules.keySet().size() > lines.size()) {
                state = LockValidationResult.LockValidationState.VALID_APPENDED;
            }
            for (String line : lines) {
                String module = line.substring(0, line.lastIndexOf(':'));
                extraModules.remove(module);
                if (mustCheckModule(module)) {
                    String version = modules.get(module);
                    if (version == null) {
                        errors.add("Lock file contained '" + line + "' but it is not part of the resolved modules");
                    } else if (!line.contains(version)) {
                        errors.add("Lock file expected '" + line + "' but resolution result was '" + module + ":" + version + "'");
                    }
                }
            }
            if (!errors.isEmpty()) {
                state = LockValidationResult.LockValidationState.INVALID;
            }
        }
        return new LockValidationResult(state, errors, extraModules);
    }

    private boolean mustCheckModule(String module) {
        return !dataExchanger.getUpgradeModules().contains(module);
    }

    private Map<String, String> getMapOfResolvedDependencies(ResolvableDependencies resolvableDependencies) {
        Map<String, String> modules = new TreeMap<String, String>();
        for (ResolvedComponentResult resolvedComponentResult : resolvableDependencies.getResolutionResult().getAllComponents()) {
            if (resolvedComponentResult.getId() instanceof ModuleComponentIdentifier) {
                ModuleComponentIdentifier id = (ModuleComponentIdentifier) resolvedComponentResult.getId();
                modules.put(id.getGroup() + ":" + id.getModule(), id.getVersion());
            }
        }
        LOGGER.warn("Found the following modules:\n\t{}", modules);
        return modules;
    }


    static class LockValidationResult {
        enum LockValidationState {
            VALID,
            INVALID,
            VALID_APPENDED,
            NO_LOCK
        }

        final LockValidationState state;
        final List<String> errors;
        final Set<String> extraModules;

        private LockValidationResult(LockValidationState state, List<String> errors, Set<String> extraModules) {
            this.state = state;
            this.errors = errors;
            this.extraModules = extraModules;
        }
    }
}
