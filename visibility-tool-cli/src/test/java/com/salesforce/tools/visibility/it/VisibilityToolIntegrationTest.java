/*-
 * Copyright (c) {year} Salesforce.
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
package com.salesforce.tools.visibility.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

/**
 *
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class VisibilityToolIntegrationTest {

    private static Path workingDirectory;

    @BeforeAll
    static void initialize() throws IOException {
        workingDirectory = Paths.get(System.getProperty("user.dir"));
        assertTrue(
            workingDirectory.endsWith("bazel_visibility_tool"),
            "This test must be run using bazel test, which is expected to make the 'bazel_visibility_tool' runfiles tree the test working directory!");
    }

}
