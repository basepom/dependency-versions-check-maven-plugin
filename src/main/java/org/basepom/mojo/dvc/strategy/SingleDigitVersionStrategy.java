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

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import javax.inject.Named;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Single Digit, may have a prefix. Assume that larger numbers are backwards compatible.
 * <p>
 * e.g. used for google guava.
 */
@Named("single-digit")
@Singleton
public class SingleDigitVersionStrategy
        implements Strategy
{
    @Override
    public String getName()
    {
        return "single-digit";
    }

    @Override
    public boolean isCompatible(final ComparableVersion expectedVersion, final ComparableVersion resolvedVersion)
    {
        final ArtifactVersion aprExpectedVersion = new DefaultArtifactVersion(checkNotNull(expectedVersion, "expectedVersion is null").getCanonical());
        final ArtifactVersion aprResolvedVersion = new DefaultArtifactVersion(checkNotNull(resolvedVersion, "resolvedVersion is null").getCanonical());

        return aprResolvedVersion.getMajorVersion() >= aprExpectedVersion.getMajorVersion();
    }
}
