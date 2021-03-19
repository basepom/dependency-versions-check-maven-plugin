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
package org.basepom.mojo.dvc;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;

import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A qualified name for a dependency or artifact. This is everything but a version.
 */
public final class QualifiedName
        implements Comparable<QualifiedName>
{
    private final String groupId;
    private final String artifactId;
    private final String type;
    private final String classifier;

    public static QualifiedName fromDependencyNode(final DependencyNode dependencyNode)
    {
        checkNotNull(dependencyNode, "dependency is null");
        return fromArtifact(RepositoryUtils.toArtifact(dependencyNode.getArtifact()));
    }

    public static QualifiedName fromDependency(final Dependency dependency)
    {
        checkNotNull(dependency, "dependency is null");
        return fromArtifact(RepositoryUtils.toArtifact(dependency.getArtifact()));
    }

    public static QualifiedName fromArtifact(final Artifact artifact)
    {
        checkNotNull(artifact, "artifact is null");

        return new QualifiedName(artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getType(),
                artifact.getClassifier());
    }

    public static QualifiedName fromProject(final MavenProject project)
    {
        checkNotNull(project, "project is null");

        return new QualifiedName(project.getGroupId(),
                project.getArtifactId(),
                null,
                null);
    }

    @VisibleForTesting
    QualifiedName(String groupId, String artifactId, String type, String classifier)
    {
        this.groupId = checkNotNull(groupId, "groupId is null");
        this.artifactId = checkNotNull(artifactId, "artifactId is null");
        this.type = type;
        this.classifier = classifier;

        checkState(classifier == null || type != null, "Classifier must be null if type is null");
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public Optional<String> getType()
    {
        return Optional.ofNullable(type);
    }

    public Optional<String> getClassifier()
    {
        return Optional.ofNullable(classifier);
    }

    /**
     * @return True if this qualified name refers to a test artifact.
     */
    public boolean hasTests()
    {
        return getType().map(t -> t.equals("test-jar")).orElse(false)
                || (getClassifier().map(c -> c.equals("tests")).orElse(false) && getType().map(t -> t.equals("jar")).orElse(false));
    }

    /**
     * @return The full name (group, artifact, type, classifier). Normalizes any test jar to be group:artifact:jar:tests.
     */
    public String getFullName()
    {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        builder.add(getGroupId()).add(getArtifactId());

        getType().ifPresent(t -> builder.add(t));
        getClassifier().ifPresent(t -> builder.add(t));
        return Joiner.on(':').join(builder.build());
    }

    /**
     * @return The short name (group, artifact, optional classifier). Skips absent classifiers. Normalizes test jars to `tests` classifier.
     */
    public String getShortName()
    {
        String result = Joiner.on(':').skipNulls()
                .join(getGroupId(),
                        getArtifactId());

        String classifier = hasTests() ? "tests" : getClassifier().orElse("");

        if (!classifier.isEmpty()) {
            result = result + " (" + classifier + ")";
        }

        return result;
    }

    public int length()
    {
        return getShortName().length();
    }

    public String getMinimalName()
    {
        return Joiner.on(':')
                .join(getGroupId(), getArtifactId());
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
        QualifiedName that = (QualifiedName) o;
        if (Objects.equals(groupId, that.groupId) &&
                Objects.equals(artifactId, that.artifactId)) {

            // Two test artifacts test equal
            if (hasTests() && ((QualifiedName) o).hasTests()) {
                return true;
            }

            return Objects.equals(getType().orElse("jar"), that.getType().orElse("jar")) &&
                    Objects.equals(getClassifier().orElse(""), that.getClassifier().orElse(""));
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        if (hasTests()) {
            return Objects.hash(groupId, artifactId, "test-jar", "tests");
        }
        else {
            return Objects.hash(groupId, artifactId, getType().orElse("jar"), getClassifier().orElse(""));
        }
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("groupId", groupId)
                .add("artifactId", artifactId)
                .add("type", type)
                .add("classifier", classifier)
                .toString();
    }

    @Override
    public int compareTo(final QualifiedName other)
    {
        if (other == null) {
            return 1;
        }
        else if (other == this || equals(other)) {
            return 0;
        }
        else {
            return getMinimalName().compareTo(other.getMinimalName());
        }
    }
}
