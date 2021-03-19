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

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;
import org.basepom.mojo.dvc.Context;
import org.basepom.mojo.dvc.PluginLog;
import org.basepom.mojo.dvc.QualifiedName;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.transfer.ArtifactTransferException;
import org.eclipse.aether.transfer.NoRepositoryLayoutException;
import org.eclipse.aether.util.artifact.JavaScopes;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

/**
 * Builds a map of dependencies required by a specific project or another dependency.
 */
public final class DependencyMapBuilder
{
    private static final PluginLog LOG = new PluginLog(DependencyMapBuilder.class);

    private final Context context;

    public DependencyMapBuilder(final Context context)
    {
        this.context = checkNotNull(context, "context is null");
    }

    /**
     * Create a map of dependencies for a given dependency node (representing an element on the dependency tree).
     *
     * @param dependencyNode The dependency node to use.
     * @param projectScopeFilter A scope limiting filter to mask out dependencies out of scope.
     * @return A map of dependencies for this given dependency node.
     *
     * @throws MojoExecutionException Dependency resolution failed.
     * @throws ProjectBuildingException Maven project could not be built.
     */
    public DependencyMap mapDependency(final DependencyNode dependencyNode,
            final DependencyFilter projectScopeFilter)
            throws MojoExecutionException, ProjectBuildingException
    {
        checkNotNull(dependencyNode, "dependencyNode is null");
        checkNotNull(projectScopeFilter, "projectScopeFilter is null");

        // build the project
        final ProjectBuildingResult result = context.getProjectBuilder().build(convertFromAetherDependency(dependencyNode), false, context.createProjectBuildingRequest());

        // now resolve the project representing the dependency.
        final MavenProject project = result.getProject();
        return mapProject(project, projectScopeFilter);
    }

    /**
     * Create a map of names to dependencies for a given project.
     *
     * @param project The current maven project.
     * @param scopeFilter A scope limiting filter to mask out dependencies out of scope.
     *
     * @return A map of dependencies for this given dependency node.
     *
     * @throws MojoExecutionException Dependency resolution failed.
     */
    public DependencyMap mapProject(final MavenProject project,
            final DependencyFilter scopeFilter)
            throws MojoExecutionException
    {
        checkNotNull(project, "project is null");
        checkNotNull(scopeFilter, "scopeFilter is null");

        final DependencyResolutionRequest request = new DefaultDependencyResolutionRequest();
        request.setRepositorySession(context.createProjectBuildingRequest().getRepositorySession());
        request.setMavenProject(project);
        request.setResolutionFilter(scopeFilter);

        DependencyResolutionResult result;

        try {
            result = context.getProjectDependenciesResolver().resolve(request);
        }
        catch (DependencyResolutionException e) {
            result = e.getResult();
            // try to resolve using the reactor projects
            final ImmutableSet<ProjectKey> reactorProjects = context.getReactorProjects().stream()
                    .map(ProjectKey::fromProject).collect(toImmutableSet());

            // resolve all dependencies that are matched by the reactor.
            final ImmutableSet<Dependency> reactorDependencies = result.getUnresolvedDependencies().stream()
                    .filter(d -> reactorProjects.contains(ProjectKey.fromDependency(d)))
                    .collect(toImmutableSet());

            result.getUnresolvedDependencies().removeAll(reactorDependencies);
            result.getResolvedDependencies().addAll(reactorDependencies);

            if (!context.isUnresolvedSystemArtifactsFailBuild()) {
                final ImmutableSet<Dependency> systemDependencies = result.getUnresolvedDependencies().stream()
                        .filter(d -> JavaScopes.SYSTEM.equals(d.getScope()))
                        .collect(toImmutableSet());

                result.getUnresolvedDependencies().removeAll(systemDependencies);
                result.getResolvedDependencies().addAll(systemDependencies);
            }

            if (!result.getUnresolvedDependencies().isEmpty()) {
                final Throwable t = Throwables.getRootCause(e);
                RemoteRepository repository = null;

                if (t instanceof NoRepositoryLayoutException) {
                    repository = ((NoRepositoryLayoutException) t).getRepository();
                }
                else if (t instanceof ArtifactTransferException) {
                    repository = ((ArtifactTransferException) t).getRepository();
                }

                if (repository != null && "legacy".equals(repository.getContentType())) {
                    LOG.warn("Could not access a legacy repository for artifacts:  %s; Reason: %s", result.getUnresolvedDependencies(), t.getMessage());
                }
                else {
                    throw new MojoExecutionException("Could not resolve the following dependencies: " + result.getUnresolvedDependencies(), e);
                }
            }
        }

        final DependencyNode graph = result.getDependencyGraph();
        final ImmutableMap.Builder<QualifiedName, DependencyNode> dependencyCollector = ImmutableMap.builder();
        final ImmutableMap<QualifiedName, DependencyNode> directDependencies = loadDependencyTree(graph, scopeFilter, dependencyCollector);
        dependencyCollector.putAll(directDependencies);
        return new DependencyMap(dependencyCollector.build(), directDependencies);
    }

    private ImmutableMap<QualifiedName, DependencyNode> loadDependencyTree(final DependencyNode node,
            final DependencyFilter filter,
            final ImmutableMap.Builder<QualifiedName, DependencyNode> allDependencyCollector)
    {
        final ImmutableMap.Builder<QualifiedName, DependencyNode> builder = ImmutableMap.builder();
        for (final DependencyNode dependencyNode : node.getChildren()) {
            if (dependencyNode.getManagedBits() != 0) {
                if ((dependencyNode.getManagedBits() & DependencyNode.MANAGED_VERSION) != 0) {
                    LOG.debug("%s -> Managed Version!", dependencyNode.getArtifact());
                }
                if ((dependencyNode.getManagedBits() & DependencyNode.MANAGED_SCOPE) != 0) {
                    LOG.debug("%s -> Managed Scope!", dependencyNode.getArtifact());
                }
                if ((dependencyNode.getManagedBits() & DependencyNode.MANAGED_OPTIONAL) != 0) {
                    LOG.debug("%s -> Managed Optional!", dependencyNode.getArtifact());
                }
                if ((dependencyNode.getManagedBits() & DependencyNode.MANAGED_PROPERTIES) != 0) {
                    LOG.debug("%s -> Managed Properties!", dependencyNode.getArtifact());
                }
                if ((dependencyNode.getManagedBits() & DependencyNode.MANAGED_EXCLUSIONS) != 0) {
                    LOG.debug("%s -> Managed Exclusions!", dependencyNode.getArtifact());
                }
            }

            if (filter.accept(dependencyNode, ImmutableList.of(node))) {
                final QualifiedName name = QualifiedName.fromDependencyNode(dependencyNode);
                builder.put(name, dependencyNode);

                allDependencyCollector.putAll(loadDependencyTree(dependencyNode, filter, allDependencyCollector));
            }
        }
        return builder.build();
    }

    static org.apache.maven.artifact.Artifact convertFromAetherDependency(final DependencyNode dependencyNode)
    {
        Artifact aetherArtifact = convertToPomArtifact(dependencyNode.getArtifact());

        final org.apache.maven.artifact.Artifact mavenArtifact = RepositoryUtils.toArtifact(aetherArtifact);
        mavenArtifact.setScope(dependencyNode.getDependency().getScope());
        mavenArtifact.setOptional(dependencyNode.getDependency().isOptional());

        return mavenArtifact;
    }

    static Artifact convertToPomArtifact(final Artifact artifact) {
        // pom artifact has no classifier. If this is already a pom artifact, don't touch it.
        if (artifact.getClassifier().isEmpty() && "pom".equals(artifact.getExtension())) {
            return artifact;
        }

        // create a POM artifact.
        return new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), "pom", artifact.getVersion());
    }

    private static final class ProjectKey
    {
        private final String groupId;
        private final String artifactId;
        private final String version;

        public static ProjectKey fromProject(final MavenProject project)
        {
            checkNotNull(project, "project; is null");
            return new ProjectKey(project.getGroupId(), project.getArtifactId(), project.getVersion());
        }

        public static ProjectKey fromDependency(final Dependency dependency)
        {
            checkNotNull(dependency, "artifact; is null");
            return new ProjectKey(dependency.getArtifact().getGroupId(),
                    dependency.getArtifact().getArtifactId(),
                    dependency.getArtifact().getVersion());
        }

        private ProjectKey(final String groupId, final String artifactId, final String version)
        {
            this.groupId = checkNotNull(groupId, "groupId is null");
            this.artifactId = checkNotNull(artifactId, "artifactId is null");
            this.version = checkNotNull(version, "version is null");
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ProjectKey that = (ProjectKey) o;
            return groupId.equals(that.groupId) &&
                    artifactId.equals(that.artifactId) &&
                    version.equals(that.version);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(groupId, artifactId, version);
        }
    }
}
