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

import org.basepom.mojo.dvc.QualifiedName;

import java.util.Objects;

import com.google.common.base.MoreObjects;

public final class VersionResolutionElement
        implements Comparable<VersionResolutionElement> {

    /**
     * Requesting Dependency name.
     */
    private final QualifiedName requestingDependency;

    /**
     * True if the dependency was managed to this version.
     */
    private final boolean managedDependency;

    /**
     * True if this version resolution is directly from the root project.
     */
    private final boolean directDependency;

    private boolean conflict = false;

    VersionResolutionElement(final QualifiedName requestingDependency, final boolean managedDependency, final boolean directDependency) {
        this.requestingDependency = requestingDependency;
        this.managedDependency = managedDependency;
        this.directDependency = directDependency;
    }

    public QualifiedName getRequestingDependency() {
        return requestingDependency;
    }

    public boolean isManagedDependency() {
        return managedDependency;
    }

    public boolean isDirectDependency() {
        return directDependency;
    }

    public void conflict() {
        this.conflict = true;
    }

    public boolean hasConflict() {
        return conflict;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final VersionResolutionElement that = (VersionResolutionElement) o;
        return managedDependency == that.managedDependency
                && directDependency == that.directDependency
                && requestingDependency.equals(that.requestingDependency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestingDependency, managedDependency, directDependency);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("requestingDependency", requestingDependency)
                .add("managedDependency", managedDependency)
                .add("directDependency", directDependency)
                .add("conflict", conflict)
                .toString();
    }

    @Override
    public int compareTo(final VersionResolutionElement other) {
        if (other == null) {
            return 1;
        } else if (other == this || equals(other)) {
            return 0;
        } else {
            return getRequestingDependency().getMinimalName().compareTo(other.getRequestingDependency().getMinimalName());
        }
    }
}
