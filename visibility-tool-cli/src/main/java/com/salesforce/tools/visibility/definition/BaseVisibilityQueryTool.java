/*-
 * Copyright (c) 2024 Salesforce.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.salesforce.tools.visibility.definition;

import static java.lang.String.format;
import static java.nio.file.Files.isDirectory;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

import com.google.idea.blaze.base.model.primitives.TargetExpression;
import com.google.idea.blaze.base.model.primitives.WorkspacePath;
import com.salesforce.tools.visibility.util.bazel.BazelCommandExecutor;

public abstract class BaseVisibilityQueryTool {

    public static final String TOOLS_BUILD_VISIBILITY = "//tools/build/visibility";

    private final Path visibilityPackageDir;
    private final WorkspacePath visibilityPackage;
    private final BazelCommandExecutor executor;

    public BaseVisibilityQueryTool(TargetExpression visibilityPackage, Path workspaceRoot,
            BazelCommandExecutor executor) {
        this.executor = executor;
        if (visibilityPackage == null) {
            visibilityPackage = requireNonNull(TargetExpression.fromStringSafe(TOOLS_BUILD_VISIBILITY));
        } else if (visibilityPackage.toString().endsWith("/...")) {
            throw new IllegalArgumentException(
                    "Invalid visibility package: please remove '/...' suffix! It will be added automatically");
        }

        this.visibilityPackage = new WorkspacePath(visibilityPackage.toString().substring(2));
        visibilityPackageDir = workspaceRoot.resolve(this.visibilityPackage.asPath());
        if (!isDirectory(visibilityPackageDir)) {
            throw new IllegalArgumentException(
                    format(
                        "Visibility package '%s' not found. Please create it or use '--visibility-package' option to point to the correct visibility directory in workspace '%s'.",
                        visibilityPackage,
                        workspaceRoot));
        }
    }

    protected BazelCommandExecutor getExecutor() {
        return executor;
    }

    public WorkspacePath getVisibilityPackage() {
        return visibilityPackage;
    }

    public Path getVisibilityPackageDir() {
        return visibilityPackageDir;
    }

    protected TargetExpression getVisibilityPackageQueryScope() {
        return TargetExpression.allFromPackageRecursive(getVisibilityPackage());
    }

}