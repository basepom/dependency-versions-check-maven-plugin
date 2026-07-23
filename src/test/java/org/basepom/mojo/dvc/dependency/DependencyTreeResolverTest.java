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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.basepom.mojo.dvc.ScopeLimitingFilter;

import java.util.List;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.junit.jupiter.api.Test;

public class DependencyTreeResolverTest {

    @Test
    public void testSelectDependencyScopePrefersEffectiveScope() {
        assertEquals(JavaScopes.PROVIDED, DependencyTreeResolver.selectDependencyScope(JavaScopes.PROVIDED, JavaScopes.COMPILE));
    }

    @Test
    public void testSelectDependencyScopeFallsBackToDeclaredScope() {
        assertEquals(JavaScopes.RUNTIME, DependencyTreeResolver.selectDependencyScope(null, JavaScopes.RUNTIME));
    }

    @Test
    public void testSelectDependencyScopeDefaultsToCompile() {
        assertEquals(JavaScopes.COMPILE, DependencyTreeResolver.selectDependencyScope(null, null));
    }

    @Test
    public void testCreateTransitiveScopeFilterUsesEffectiveScopeFromResolvedNode() {
        final Dependency declaredDependency = new Dependency(new DefaultArtifact("test:declared:1.0"), JavaScopes.COMPILE);
        final DependencyNode resolvedDependencyNode = new DefaultDependencyNode(
                new Dependency(new DefaultArtifact("test:declared:1.0"), JavaScopes.PROVIDED));

        final ScopeLimitingFilter filter = DependencyTreeResolver.createTransitiveScopeFilter(resolvedDependencyNode, declaredDependency);

        assertTrue(filter.accept(new DefaultDependencyNode(new Dependency(new DefaultArtifact("test:runtime:1.0"), JavaScopes.RUNTIME)), List.of()));
        assertFalse(filter.accept(new DefaultDependencyNode(new Dependency(new DefaultArtifact("test:test:1.0"), JavaScopes.TEST)), List.of()));
    }
}
