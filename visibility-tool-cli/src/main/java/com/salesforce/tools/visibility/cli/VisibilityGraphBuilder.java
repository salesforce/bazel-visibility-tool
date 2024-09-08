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

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.salesforce.tools.visibility.definition.VisibilityGroup;

/**
 * A tool for analyzing visibility information
 */
public class VisibilityGraphBuilder {

    private final ImmutableGraph<VisibilityGroup> graph;
    private final SortedMap<String, VisibilityGroup> groupsSortedByName = new TreeMap<>();

    public VisibilityGraphBuilder(Collection<VisibilityGroup> visibilityGroups) {
        for (VisibilityGroup group : visibilityGroups) {
            groupsSortedByName.put(group.getName(), group);
        }

        ImmutableGraph.Builder<VisibilityGroup> graphBuilder = GraphBuilder.directed()
                .allowsSelfLoops(true)
                .expectedNodeCount(groupsSortedByName.size())
                .<VisibilityGroup> immutable();
        for (VisibilityGroup group : groupsSortedByName.values()) {
            graphBuilder.addNode(group);

            var visibleToGroups = group.getVisibleToGroups();
            for (String ref : visibleToGroups) {
                var name = ref.startsWith(":") ? ref.substring(1) : ref;
                var target = requireNonNull(
                    groupsSortedByName.get(name),
                    () -> format(
                        "Invalid reference in visible_to_groups attribute of group '%s': group '%s' is not defined!",
                        group.getName(),
                        name));
                graphBuilder.putEdge(group, target);
            }
        }
        graph = graphBuilder.build();
    }

    public ImmutableGraph<VisibilityGroup> getGraph() {
        return graph;
    }
}
