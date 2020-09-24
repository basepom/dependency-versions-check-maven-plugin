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
package org.basepom.mojo.dvc.version;

import com.google.common.base.MoreObjects;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.basepom.mojo.dvc.QualifiedName;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public final class VersionResolution
{
    /**
     * The dependencies that requests this specific version resolution.
     */
    private final VersionResolutionElement requestingDependency;

    /**
     * The version expected by the requesting dependency.
     */
    private final ComparableVersion expectedVersion;

    public static VersionResolution forDirectDependency(final QualifiedName requestingDependency,
            final ComparableVersion expectedVersion,
            final boolean managedDependency)
    {
        return new VersionResolution(requestingDependency, expectedVersion, managedDependency, true);
    }

    public static VersionResolution forTransitiveDependency(final QualifiedName requestingDependency,
            final ComparableVersion expectedVersion,
            final boolean managedDependency)
    {
        return new VersionResolution(requestingDependency, expectedVersion, managedDependency, false);
    }

    private VersionResolution(
            final QualifiedName requestingDependency,
            final ComparableVersion expectedVersion,
            final boolean manageDependency,
            final boolean directDependency)
    {
        checkNotNull(requestingDependency, "requestingDependencyName is null");
        this.requestingDependency = new VersionResolutionElement(requestingDependency, manageDependency, directDependency);
        this.expectedVersion = checkNotNull(expectedVersion, "expectedVersion is null");
    }

    public VersionResolutionElement getRequestingDependency()
    {
        return requestingDependency;
    }

    public ComparableVersion getExpectedVersion()
    {
        return expectedVersion;
    }

    public void conflict()
    {
        requestingDependency.conflict();
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
        final VersionResolution that = (VersionResolution) o;
        return requestingDependency.equals(that.requestingDependency) &&
                expectedVersion.equals(that.expectedVersion);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(requestingDependency, expectedVersion);
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("requestingDependency", requestingDependency)
                .add("expectedVersion", expectedVersion)
                .toString();
    }
}
