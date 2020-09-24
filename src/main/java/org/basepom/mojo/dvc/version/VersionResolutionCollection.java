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
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.basepom.mojo.dvc.QualifiedName;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.ImmutableSetMultimap.toImmutableSetMultimap;

public final class VersionResolutionCollection
        implements Comparable<VersionResolutionCollection>
{
    private final ImmutableSortedSet<VersionResolutionElement> requestingDependencies;
    private final ComparableVersion expectedVersion;

    private VersionResolutionCollection(Map.Entry<ComparableVersion, Collection<VersionResolutionElement>> entry)
    {
        checkNotNull(entry, "entry is null");
        this.expectedVersion = entry.getKey();
        this.requestingDependencies = ImmutableSortedSet.copyOf(entry.getValue());
    }

    public ImmutableSortedSet<VersionResolutionElement> getRequestingDependencies()
    {
        return requestingDependencies;
    }

    public ComparableVersion getExpectedVersion()
    {
        return expectedVersion;
    }

    /**
     * True if the selected version exactly matches the expected version.
     */
    public boolean isMatchFor(ComparableVersion version)
    {
        checkNotNull(version, "version is null");
        return expectedVersion.getCanonical().equals(version.getCanonical());
    }

    @Override
    public int compareTo(final VersionResolutionCollection other)
    {
        if (other == null) {
            return 1;
        }
        else if (other == this || equals(other)) {
            return 0;
        }
        else {
            // order by expected version
            return this.getExpectedVersion().compareTo(other.getExpectedVersion());
        }
    }

    public boolean hasConflict()
    {
        return requestingDependencies.stream().anyMatch(VersionResolutionElement::hasConflict);
    }

    public boolean hasDirectDependencies()
    {
        return requestingDependencies.stream().anyMatch(VersionResolutionElement::isDirectDependency);
    }

    public boolean hasManagedDependencies()
    {
        return requestingDependencies.stream().anyMatch(VersionResolutionElement::isManagedDependency);
    }

    public static ImmutableSetMultimap<QualifiedName, VersionResolutionCollection> toResolutionMap(final SetMultimap<QualifiedName, VersionResolution> map)
    {
        final ImmutableSetMultimap.Builder<QualifiedName, VersionResolutionCollection> builder = ImmutableSetMultimap.builder();
        builder.orderKeysBy(QualifiedName::compareTo).orderValuesBy(VersionResolutionCollection::compareTo);

        Maps.transformValues(map.asMap(), v -> {
            // fold into Map from version -> set of all dependencies that want this version.
            final ImmutableSetMultimap<ComparableVersion, VersionResolutionElement> versionMap = v.stream()
                    .collect(toImmutableSetMultimap(VersionResolution::getExpectedVersion, VersionResolution::getRequestingDependency));
            // collect result into a set of VersionResolutionCollection objects.
            return versionMap.asMap().entrySet().stream()
                    .map(VersionResolutionCollection::new)
                    .collect(toImmutableSet());
            // stream the entries of QualifiedName, Set of version resolution collections
            // collect them into a Multimap from QualifiedName to version resolution collections.
        }).forEach(builder::putAll);

        return builder.build();
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("requestingDependencies", requestingDependencies)
                .add("expectedVersion", expectedVersion)
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
        final VersionResolutionCollection that = (VersionResolutionCollection) o;
        return requestingDependencies.equals(that.requestingDependencies) &&
                expectedVersion.equals(that.expectedVersion);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(requestingDependencies, expectedVersion);
    }
}
