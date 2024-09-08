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
package com.salesforce.tools.visibility.cli;

import static java.nio.file.Files.isReadable;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.salesforce.tools.visibility.definition.VisibilityGroup;
import com.salesforce.tools.visibility.definition.VisibilityGroupInfoQueryTool;
import com.salesforce.tools.visibility.util.bazel.BazelBinary;
import com.salesforce.tools.visibility.util.bazel.BazelVersion;
import com.salesforce.tools.visibility.util.bazel.DefaultBazelCommandExecutor;

@Disabled("data dependency has issues because of sandboxing; this cannot be run sandboxed in Bazel")
public class VisibilityGroupInfoLoaderTest {

    private static Path workspaceDirectory;
    private static VisibilityGroupInfoQueryTool visibilityGroupInfoLoader;

    @BeforeAll
    static void initialize() throws IOException {
        workspaceDirectory = Path.of("visibility-tool-cli/src/test/workspace");
        assertTrue(
            isReadable(workspaceDirectory.resolve("tools/build/visibility/BUILD.bazel")),
            "Please check the 'data' configuration of the unit test and the working directory. Bazel test will execute the test in runfiles tree of the 'bazel_visibility_tool' workspace.");

        var executor = new DefaultBazelCommandExecutor();
        executor.setBazelBinary(new BazelBinary(Path.of("bazel"), new BazelVersion(7, 0, 0)));

        visibilityGroupInfoLoader = new VisibilityGroupInfoQueryTool(null, workspaceDirectory, executor);
        assertNotNull(visibilityGroupInfoLoader.getVisibilityPackageDir());
        assertNotNull(visibilityGroupInfoLoader.getVisibilityGroups());
    }

    private Collection<VisibilityGroup> visibilityGroups;

    @BeforeEach
    void load() throws IOException {
        visibilityGroups = Collections.unmodifiableCollection(visibilityGroupInfoLoader.getVisibilityGroups());
    }

    @Test
    void load_visibility_info_from_test_workspace() throws Exception {
        Map<String, VisibilityGroup> groupsByName =
                visibilityGroups.stream().collect(toMap(VisibilityGroup::getName, Function.identity()));
        assertFalse(groupsByName.isEmpty());

        var jakartaMigrationGroup = groupsByName.get("lib-javax-jakarata-migration");
        assertNotNull(jakartaMigrationGroup, "Expected group 'lib-javax-jakarata-migration' not found!");
        assertNotNull(jakartaMigrationGroup.getVisibilityAllowList(), "Expected to have an allow list defined!");

        var appBackendApiGroup = groupsByName.get("app-backend-api");
        assertNotNull(appBackendApiGroup, "Expected group 'app-backend-api' not found!");
    }
}
