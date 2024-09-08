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
package com.salesforce.tools.visibility.deps_tool_ext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.idea.blaze.base.model.primitives.Label;
import com.salesforce.tools.visibility.definition.MavenDepsVisibilityInfo;

public class MavenDepsVisibilityAnalyzerTest {

    @Test
    void excludePatterns_match() throws Exception {
        var a = new MavenDepsVisibilityAnalyzer(
                Stream.of(
                    new MavenDepsVisibilityInfo(
                            Label.create("//test:test1"),
                            Label.create("//tools/build/visibility:lib-foo"),
                            List.of("@*component*-api", "@component//apis/**"),
                            List.of("@prefix-*"))));

        assertNull(a.findGroupForExternalRepository("prefix-component-foobar-api"));
        assertNull(a.findGroupForExternalRepository("@prefix-component-foobar-api"));

        assertNotNull(a.findGroupForExternalRepository("-component-foobar-api"));
    }

    @Test
    void multiple_excludePatterns_only_one_needs_to_match() throws Exception {
        var a = new MavenDepsVisibilityAnalyzer(
                Stream.of(
                    new MavenDepsVisibilityInfo(
                            Label.create("//test:test1"),
                            Label.create("//tools/build/visibility:lib-foo"),
                            List.of("@*component*-api", "@component//apis/**"),
                            List.of("@prefix-*", "@somethingelse*"))));

        assertNull(a.findGroupForExternalRepository("prefix-component-foobar-api"));
        assertNull(a.findGroupForExternalRepository("@prefix-component-foobar-api"));

        assertNotNull(a.findGroupForExternalRepository("-component-foobar-api"));
    }
}
