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

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import com.salesforce.tools.bazel.mavendependencies.starlark.StarlarkStringBuilder;

/**
 * A <code>package_group</code> definition.
 */
public class PackageGroup {

    private final String name;
    private final SortedSet<String> packages;
    private final SortedSet<String> includes;

    public PackageGroup(String name, Collection<String> packages, Collection<String> includes) {
        this.name = name;
        this.packages = packages != null ? new TreeSet<>(packages) : new TreeSet<>();
        this.includes = includes != null ? new TreeSet<>(includes) : new TreeSet<>();
    }

    public void appendTo(StarlarkStringBuilder output) {
        output.append("package_group(").appendNewline().increaseIndention();
        output.appendAttribute("name", name).appendCommaFollowedByNewline();
        if (!includes.isEmpty()) {
            output.appendListAttribute("includes", includes).appendCommaFollowedByNewline();
        }
        if (!packages.isEmpty()) {
            output.appendListAttribute("packages", packages).appendCommaFollowedByNewline();
        }
        output.decreaseIndention();
        output.append(")").appendNewline();
    }

    /**
     * @return the includes
     */
    public SortedSet<String> getIncludes() {
        return this.includes;
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the packages
     */
    public SortedSet<String> getPackages() {
        return this.packages;
    }
}
