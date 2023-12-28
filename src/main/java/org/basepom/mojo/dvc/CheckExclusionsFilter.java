/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.basepom.mojo.dvc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;

public final class CheckExclusionsFilter
        implements DependencyFilter {

    private final List<Exclusion> exclusions;

    public CheckExclusionsFilter(final Collection<Exclusion> exclusions) {
        this.exclusions = ImmutableList.copyOf(checkNotNull(exclusions, "exclusions is null"));
    }

    @Override
    @SuppressWarnings("PMD.AvoidBranchingStatementAsLastInLoop")
    public boolean accept(final DependencyNode node, final List<DependencyNode> parents) {
        checkNotNull(node, "node is null");
        for (final Exclusion exclusion : exclusions) {
            // drop wildcards.
            if ("*".equals(exclusion.getArtifactId()) && "*".equals(exclusion.getGroupId())) {
                return false;
            }
            // return false if both artifact id and group id are equal
            return !(Objects.equals(exclusion.getArtifactId(), node.getArtifact().getArtifactId())
                    && Objects.equals(exclusion.getGroupId(), node.getArtifact().getGroupId()));
        }

        return true; // no exclusion found.
    }
}
