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
package org.basepom.mojo.dvc.model;

import com.google.common.base.MoreObjects;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.basepom.mojo.dvc.QualifiedName;
import org.basepom.mojo.dvc.QualifiedNameMatcher;
import org.eclipse.aether.graph.DependencyNode;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Mojo model, adds an exclude to the checker.
 */
public class VersionCheckExcludes
{
    private String dependency = "";
    private ComparableVersion expected;
    private ComparableVersion resolved;

    public void setDependency(final String dependency)
    {
        this.dependency = checkNotNull(dependency, "dependency is null").trim();
    }

    public void setExpected(final String version)
    {
        checkNotNull(version, "version is null");
        this.expected = new ComparableVersion(version);
    }

    public void setResolved(final String version)
    {
        checkNotNull(version, "version is null");
        this.resolved = new ComparableVersion(version);
    }

    public boolean isValid()
    {
        return (expected != null)
                && (resolved != null);
    }

    public boolean matches(final DependencyNode dependencyNode, final ComparableVersion expectedVersion, final ComparableVersion resolvedVersion)
    {
        checkNotNull(dependencyNode, "dependencyNode is null");
        checkNotNull(expectedVersion, "expectedVersion is null");
        checkNotNull(resolvedVersion, "resolvedVersion is null");

        final QualifiedNameMatcher matcher = new QualifiedNameMatcher(dependency);
        final QualifiedName artifactName = QualifiedName.fromDependencyNode(dependencyNode);

        return matcher.matches(artifactName)
                && this.expected.equals(expectedVersion)
                && this.resolved.equals(resolvedVersion);
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("dependency", dependency)
                .add("expected", expected)
                .add("resolved", resolved)
                .toString();
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VersionCheckExcludes that = (VersionCheckExcludes) o;
        return Objects.equals(dependency, that.dependency) &&
                Objects.equals(expected, that.expected) &&
                Objects.equals(resolved, that.resolved);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(dependency, expected, resolved);
    }
}
