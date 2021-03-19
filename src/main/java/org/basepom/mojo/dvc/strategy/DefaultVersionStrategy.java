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

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.codehaus.plexus.component.annotations.Component;

/**
 * This is the default versioning strategy used by previous versions of the plugin.
 * It assumes that all smaller versions are compatible when replaced with larger numbers and compares version
 * elements from left to right. E.g. 3.2.1 &gt; 3.2 and 2.1.1 &gt; 1.0. Usually works pretty ok.
 */
@Component(role = Strategy.class, hint = "default")
public class DefaultVersionStrategy
        implements Strategy
{
    @Override
    public String getName()
    {
        return "default";
    }

    @Override
    public final boolean isCompatible(final ComparableVersion expectedVersion, final ComparableVersion resolvedVersion)
    {
        // this is the same as converting the versions to the DefaultArtifactVersion and then do compareTo, as this
        // uses ComparableVersion under the hood.
        return resolvedVersion.compareTo(expectedVersion) >= 0;
    }
}
