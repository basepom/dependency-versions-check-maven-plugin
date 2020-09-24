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
package org.basepom.mojo.dvc.dependency;

import com.google.common.collect.ImmutableMap;
import org.basepom.mojo.dvc.QualifiedName;
import org.eclipse.aether.graph.DependencyNode;

import static com.google.common.base.Preconditions.checkNotNull;

public final class DependencyMap
{
    private final ImmutableMap<QualifiedName, DependencyNode> allDependencies;
    private final ImmutableMap<QualifiedName, DependencyNode> directDependencies;

    DependencyMap(final ImmutableMap<QualifiedName, DependencyNode> allDependencies,
            final ImmutableMap<QualifiedName, DependencyNode> directDependencies)
    {
        this.allDependencies = checkNotNull(allDependencies, "allDependencies is null");
        this.directDependencies = checkNotNull(directDependencies, "directDependencies is null");
    }

    public ImmutableMap<QualifiedName, DependencyNode> getAllDependencies()
    {
        return allDependencies;
    }

    public ImmutableMap<QualifiedName, DependencyNode> getDirectDependencies()
    {
        return directDependencies;
    }
}
