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

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import com.google.idea.blaze.base.model.primitives.Label;
import com.salesforce.tools.visibility.definition.MavenDepsVisibilityInfo;

/**
 * Analyzes bazel_maven_deps visibility information analyzer
 */
public class MavenDepsVisibilityAnalyzer {

    static class VisibilityGroupWithExcludeMatchers {

        private final MavenDepsVisibilityInfo info;
        private final Collection<PathMatcher> excludeMatchers;

        public VisibilityGroupWithExcludeMatchers(MavenDepsVisibilityInfo info,
                Collection<PathMatcher> excludeMatcher) {
            this.info = requireNonNull(info);
            this.excludeMatchers = requireNonNull(excludeMatcher).isEmpty() ? null : excludeMatcher;
        }

        public MavenDepsVisibilityInfo getInfo() {
            return info;
        }

        public boolean isExcluded(Path path) {
            if (excludeMatchers == null) {
                return false;
            }

            // it's excluded when only on watcher matches
            return excludeMatchers.parallelStream().anyMatch(p -> p.matches(path));
        }

        @Override
        public String toString() {
            if (info.getExcludePatterns().isEmpty()) {
                return info.toString();
            }

            return info + " " + info.getExcludePatterns();
        }
    }

    private final Map<PathMatcher, VisibilityGroupWithExcludeMatchers> includePatternMatcherIndex;

    public MavenDepsVisibilityAnalyzer(Stream<MavenDepsVisibilityInfo> mavenDepsVisibilityInfos) {
        this.includePatternMatcherIndex = createIncludePatternMatcherIndex(mavenDepsVisibilityInfos);
    }

    private PathMatcher createGlobMatcher(String p) {
        if (p.startsWith("@")) {
            p = p.substring(1);
        } else if (p.startsWith("//")) {
            p = p.substring(2);
            if (p.endsWith("/...")) {
                p = p.replace("/...", "/**");
            }
        }
        return FileSystems.getDefault().getPathMatcher("glob:" + p);
    }

    private Map<PathMatcher, VisibilityGroupWithExcludeMatchers> createIncludePatternMatcherIndex(
            Stream<MavenDepsVisibilityInfo> infos) {
        // we use an IdentityHashMap assuming that the glob matchers are unique for each pattern
        Map<PathMatcher, VisibilityGroupWithExcludeMatchers> result = new IdentityHashMap<>();

        infos.forEach(info -> {
            if (info.getIncludePatterns().isEmpty()) {
                return;
            }

            if (!info.getIncludePatterns().stream().allMatch(p -> p.startsWith("@"))) {
                throw new IllegalStateException(
                        format(
                            "Invalid Maven dependency visibility info '%s': all include_patterns must begin with '@'",
                            info.getDefiningTargetLabel()));
            }
            for (String includePattern : info.getIncludePatterns()) {
                var existing = result.put(
                    createGlobMatcher(includePattern),
                    new VisibilityGroupWithExcludeMatchers(
                            info,
                            info.getExcludePatterns().stream().map(this::createGlobMatcher).collect(toList())));
                if (existing != null) {
                    throw new IllegalStateException(
                            format(
                                "The JVM reuses PathMatcher instances. This code assumption is no longer true and needs to be fixed. Error adding include pattern '%s' for '%s'",
                                includePattern,
                                info.getDefiningTargetLabel()));
                }
            }
        });

        return result;
    }

    public MavenDepsVisibilityInfo findGroupForExternalRepository(String repositoryName) {
        if (repositoryName.startsWith("@@")) {
            throw new IllegalArgumentException(
                    "Invalid respository name. A canonical label is not expected here: " + repositoryName);
        }
        if (repositoryName.startsWith("@")) {
            repositoryName = repositoryName.substring(1);
        }

        var info = findInfosForPath(Path.of(repositoryName), includePatternMatcherIndex);
        if (info.size() > 1) {
            // it's not allowed to have more than two matching groups
            throw new IllegalStateException(
                    format(
                        "There is more than one bazel_maven_deps info matching external repository '%s': %s",
                        repositoryName,
                        info.stream()
                                .map(MavenDepsVisibilityInfo::getDefiningTargetLabel)
                                .map(Label::toString)
                                .collect(joining(", "))));
        }
        return info.isEmpty() ? null : info.get(0);
    }

    private List<MavenDepsVisibilityInfo> findInfosForPath(
            Path path,
            Map<PathMatcher, VisibilityGroupWithExcludeMatchers> index) {

        return index.entrySet()
                .parallelStream()
                .filter(e -> e.getKey().matches(path)) // inclusion must match
                .map(Entry::getValue)
                .filter(g -> !g.isExcluded(path)) // exclusion must not match
                .map(VisibilityGroupWithExcludeMatchers::getInfo)
                .distinct() // eliminate duplicates in case user was too greedy with patterns
                .collect(toList());
    }
}
