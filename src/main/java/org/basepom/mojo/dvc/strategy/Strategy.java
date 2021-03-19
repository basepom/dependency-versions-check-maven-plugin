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

/**
 * A strategy implementation decides whether two versions are compatible with each other.
 */
public interface Strategy
{
    /**
     * @return The name of the strategy.
     */
    String getName();

    /**
     * @param expectedVersion The artifact version expected (artifact version b).
     * @param resolvedVersion The proposed artifact version (artifact version a).
     * @return True if an artifact with Version b can be replaced by an artifact with Version a.
     */
    boolean isCompatible(ComparableVersion expectedVersion, ComparableVersion resolvedVersion);
}

