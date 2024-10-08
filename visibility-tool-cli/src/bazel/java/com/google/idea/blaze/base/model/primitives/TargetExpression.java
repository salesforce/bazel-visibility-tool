/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.google.idea.blaze.base.model.primitives;

import java.io.Serializable;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * An interface for objects that represent targets you could pass to Blaze on the command line. See {@link Label}.
 *
 * <p>
 * TargetExpression is a generalization of {@link Label} to include wildcards for finding all packages beneath some
 * root, and/or all targets within a package.
 */
public class TargetExpression implements Comparable<TargetExpression>, Serializable {
    // still Serializable as part of ProjectViewSet
    public static final long serialVersionUID = 1L;

    protected static final Pattern VALID_REPO_NAME = Pattern.compile("@[\\w\\-.]*");

    public static TargetExpression allFromPackageNonRecursive(WorkspacePath localPackage) {
        return new TargetExpression("//" + localPackage.relativePath() + ":all");
    }

    /** All targets in all packages below the given path */
    public static TargetExpression allFromPackageRecursive(WorkspacePath localPackage) {
        if (localPackage.isWorkspaceRoot()) {
            return new TargetExpression("//...:all");
        }
        return new TargetExpression("//" + localPackage.relativePath() + "/...:all");
    }

    /**
     * @return A Label instance if the expression is a valid label, or a TargetExpression instance if it is not.
     * @throws InvalidTargetException
     *             if it's not a valid blaze target pattern
     */
    public static TargetExpression fromString(String expression) throws InvalidTargetException {
        var error = validate(expression);
        if (error != null) {
            throw new InvalidTargetException(error);
        }
        return Label.validate(expression) == null ? Label.create(expression) : new TargetExpression(expression);
    }

    /** Silently returns null if this is not a valid {@link TargetExpression}. */
    @Nullable
    public static TargetExpression fromStringSafe(String expression) {
        var error = validate(expression);
        if (error != null) {
            return null;
        }
        return Label.validate(expression) == null ? Label.create(expression) : new TargetExpression(expression);
    }

    /** Parse package path from a target pattern of the form [//][packagePath][:targetName] */
    private static String getPackagePath(String targetPattern) {
        var colonIndex = targetPattern.indexOf(':');
        var prefixLength = targetPattern.startsWith("//") ? 2 : 0;
        return colonIndex >= 0 ? targetPattern.substring(prefixLength, colonIndex)
                : targetPattern.substring(prefixLength);
    }

    /** Validate the given target pattern. Returns null on success or an error message otherwise. */
    @Nullable
    public static String validate(String targetPattern) {
        if (targetPattern.isEmpty()) {
            return "Target should be non-empty.";
        }
        if (targetPattern.charAt(0) == '-') {
            targetPattern = targetPattern.substring(1);
        }

        if (targetPattern.charAt(0) == '@') {
            var slashesIndex = targetPattern.indexOf("//");
            if (slashesIndex == -1) {
                return String.format("Invalid target expression '%s': Couldn't find package path", targetPattern);
            }
            if (!VALID_REPO_NAME.matcher(targetPattern.substring(0, slashesIndex)).matches()) {
                return String.format(
                    "Invalid target expression '%s': workspace names may contain only "
                            + "A-Z, a-z, 0-9, '-', '_' and '.'",
                    targetPattern);
            }
            targetPattern = targetPattern.substring(slashesIndex);
        }
        targetPattern = WildcardTargetPattern.stripWildcardSuffix(targetPattern);

        var error = PackagePathValidator.validatePackageName(getPackagePath(targetPattern));
        if (error != null) {
            return error;
        }

        var colonIndex = targetPattern.indexOf(':');
        if (colonIndex < 0) {
            return null;
        }
        return TargetName.validate(targetPattern.substring(colonIndex + 1));
    }

    private final String expression;

    protected TargetExpression(String expression) {
        this.expression = expression;
    }

    @Override
    public int compareTo(TargetExpression o) {
        return expression.compareTo(o.expression);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TargetExpression)) {
            return false;
        }
        return expression.equals(((TargetExpression) o).expression);
    }

    @Override
    public int hashCode() {
        return expression.hashCode();
    }

    /** Is this an excluded target expression (i.e. starts with '-')? */
    public boolean isExcluded() {
        return expression.startsWith("-");
    }

    @Override
    public String toString() {
        return expression;
    }
}
