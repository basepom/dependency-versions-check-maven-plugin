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
package org.basepom.mojo.dvc.strategy;

import com.google.common.base.Strings;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.basepom.mojo.dvc.PluginLog;
import org.codehaus.plexus.component.annotations.Component;

import java.util.Comparator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implements Apache versioning strategy for two or three digits. It expects versions formatted as x.y, x.y.z. Versions
 * can have an additional qualifier.
 * <p>
 * Version A (xa.ya.za) can replace Version B (xb.yb.zb) if xa == xb and xa &gt;= xb. component z is always compatible.
 * <p>
 * If an additional qualifier exists, the qualifiers must match.
 */
@Named("apr")
@Singleton
public class AprVersionStrategy
        implements Strategy
{
    private final Comparator<ArtifactVersion> comparator = Comparator
            .comparing(ArtifactVersion::getMajorVersion, this::checkMajorCompatible)
            .thenComparing(ArtifactVersion::getMinorVersion, this::checkMinorCompatible)
            .thenComparing(ArtifactVersion::getIncrementalVersion, this::checkPatchCompatible)
            .thenComparing(ArtifactVersion::getQualifier, (a, e) -> checkQualifierCompatible(Strings.nullToEmpty(a), Strings.nullToEmpty(e)));

    @Override
    public String getName()
    {
        return "apr";
    }

    @Override
    public final boolean isCompatible(final ComparableVersion expectedVersion, final ComparableVersion resolvedVersion)
    {
        final ArtifactVersion aprExpectedVersion = new DefaultArtifactVersion(checkNotNull(expectedVersion, "expectedVersion is null").getCanonical());
        final ArtifactVersion aprResolvedVersion = new DefaultArtifactVersion(checkNotNull(resolvedVersion, "resolvedVersion is null").getCanonical());

        // Expected version must be before or equal the resolved version.
        //
        // for each method:
        //
        // -1 means incompatible.
        //  0 means more testing.
        //  1 means compatible.
        //
        return comparator.compare(aprExpectedVersion, aprResolvedVersion) >= 0; // more testing or compatible wins here.
    }

    protected int checkMajorCompatible(int expectedMajor, int resolvedMajor)
    {
        if (expectedMajor != resolvedMajor) {
            return -1; // incompatible if majors differ.
        }
        return 0; // otherwise more testing.
    }

    protected int checkMinorCompatible(int expectedMinor, int resolvedMinor)
    {
        if (resolvedMinor < expectedMinor) {
            return -1; // smaller version is not forward compatible
        }

        return 0; // otherwise more testing.
    }

    protected int checkPatchCompatible(int expectedPatch, int resolvedPatch)
    {
        return 0; // all patches are compatible, check the qualifiers
    }

    protected int checkQualifierCompatible(String expectedQualifier, String resolvedQualifier)
    {
        if (!expectedQualifier.equals(resolvedQualifier)) {
            return -1; // if qualifiers don't match, things are not compatible (this makes things like 1.2.3-android and 1.2.3-jre not compatible!
        }
        return 0;
    }
}
