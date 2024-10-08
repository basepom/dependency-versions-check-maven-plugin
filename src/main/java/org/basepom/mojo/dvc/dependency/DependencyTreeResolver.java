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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.lang.String.format;
import static org.basepom.mojo.dvc.dependency.DependencyMapBuilder.convertToPomArtifact;

import org.basepom.mojo.dvc.CheckExclusionsFilter;
import org.basepom.mojo.dvc.Context;
import org.basepom.mojo.dvc.PluginLog;
import org.basepom.mojo.dvc.QualifiedName;
import org.basepom.mojo.dvc.ScopeLimitingFilter;
import org.basepom.mojo.dvc.strategy.Strategy;
import org.basepom.mojo.dvc.version.VersionResolution;
import org.basepom.mojo.dvc.version.VersionResolutionCollection;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.util.filter.AndDependencyFilter;

public final class DependencyTreeResolver
        implements AutoCloseable {

    private static final PluginLog LOG = new PluginLog(DependencyTreeResolver.class);

    private static final int DEPENDENCY_RESOLUTION_NUM_THREADS = Runtime.getRuntime().availableProcessors() * 5;

    private final Lock collectorLock = new ReentrantLock();

    private final Context context;
    private final DependencyMap rootDependencyMap;

    private final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(DEPENDENCY_RESOLUTION_NUM_THREADS,
            new ThreadFactoryBuilder().setNameFormat("dependency-version-check-worker-%s").setDaemon(true).build()));

    public DependencyTreeResolver(final Context context, final DependencyMap rootDependencyMap) {
        this.context = checkNotNull(context, "context is null");
        this.rootDependencyMap = checkNotNull(rootDependencyMap, "rootDependencyMap is null");
    }

    @Override
    public void close() {
        MoreExecutors.shutdownAndAwaitTermination(executorService, Duration.ofSeconds(2));
    }

    /**
     * Creates a map of all dependency version resolutions used in this project in a given scope. The result is a map from names to a list of version numbers
     * used in the project, based on the element requesting the version.
     * <p>
     * If the special scope "null" is used, a superset of all scopes is used (this is used by the check mojo).
     *
     * @param project     The maven project to resolve all dependencies for.
     * @param scopeFilter Limits the scopes to resolve.
     * @return Map from qualified names to possible version resolutions.
     * @throws MojoExecutionException Parallel dependency resolution failed.
     */
    public ImmutableSetMultimap<QualifiedName, VersionResolutionCollection> computeResolutionMap(final MavenProject project,
            final ScopeLimitingFilter scopeFilter)
            throws MojoExecutionException {
        checkNotNull(project, "project is null");
        checkNotNull(scopeFilter, "scope is null");

        final ImmutableSetMultimap.Builder<QualifiedName, VersionResolution> collector = ImmutableSetMultimap.builder();
        final ImmutableList.Builder<ListenableFuture<?>> futureBuilder = ImmutableList.builder();

        boolean useParallelDependencyResolution = context.useFastResolution();
        // Map from dependency name --> list of resolutions found on the tree
        LOG.debug("Using parallel dependency resolution: %s", useParallelDependencyResolution);

        final ImmutableList<Dependency> dependencies;
        if (context.useDeepScan()) {
            LOG.debug("Running deep scan");
            dependencies = ImmutableList.copyOf(
                    rootDependencyMap.getAllDependencies().values().stream().map(DependencyNode::getDependency).collect(toImmutableList()));
        } else {
            final ArtifactTypeRegistry stereotypes = context.getRepositorySystemSession().getArtifactTypeRegistry();

            dependencies = ImmutableList.copyOf(
                    project.getDependencies().stream().map(d -> RepositoryUtils.toDependency(d, stereotypes)).collect(toImmutableList()));
        }

        final ImmutableSet.Builder<Throwable> throwableBuilder = ImmutableSet.builder();

        if (useParallelDependencyResolution) {
            for (final Dependency dependency : dependencies) {
                futureBuilder.add(executorService.submit((Callable<Void>) () -> {
                    resolveProjectDependency(dependency, scopeFilter, collector);
                    return null;
                }));
            }

            final ImmutableList<ListenableFuture<?>> futures = futureBuilder.build();

            for (final ListenableFuture<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    throwableBuilder.add(e.getCause());
                }
            }
        } else {
            for (final Dependency dependency : dependencies) {
                try {
                    resolveProjectDependency(dependency, scopeFilter, collector);
                } catch (Exception e) {
                    throwableBuilder.add(e);
                }
            }
        }

        final Set<Throwable> throwables = throwableBuilder.build();
        if (!throwables.isEmpty()) {
            throw processResolveProjectDependencyException(throwableBuilder.build());
        }

        return VersionResolutionCollection.toResolutionMap(collector.build());
    }

    private static MojoExecutionException processResolveProjectDependencyException(Set<Throwable> throwables) {
        ImmutableSet.Builder<String> failedDependenciesBuilder = ImmutableSet.builder();
        ImmutableSet.Builder<String> messageBuilder = ImmutableSet.builder();
        for (Throwable t : throwables) {
            if (t instanceof DependencyResolutionException) {
                ((DependencyResolutionException) t).getResult().getUnresolvedDependencies()
                        .forEach(d -> failedDependenciesBuilder.add(printDependency(d)));
            } else {
                messageBuilder.add(t.getMessage());
            }
        }

        String message = Joiner.on("    \n").join(messageBuilder.build());
        Set<String> failedDependencies = failedDependenciesBuilder.build();
        if (!failedDependencies.isEmpty()) {
            if (!message.isEmpty()) {
                message += "\n";
            }
            message += "Could not resolve dependencies: [" + Joiner.on(", ").join(failedDependencies) + "]";
        }
        return new MojoExecutionException(message);
    }

    private static String printDependency(Dependency d) {
        return d.getArtifact() + " [" + d.getScope() + (d.isOptional() ? ", optional" : "") + "]";
    }

    /**
     * Called for any direct project dependency. Factored out from {@link #computeResolutionMap} to allow parallel evaluation of dependencies to speed up the
     * process.
     */
    private void resolveProjectDependency(final Dependency dependency,
            final ScopeLimitingFilter visibleScopes,
            final ImmutableSetMultimap.Builder<QualifiedName, VersionResolution> collector)
            throws MojoExecutionException, DependencyResolutionException, AbstractArtifactResolutionException, VersionRangeResolutionException {
        final QualifiedName dependencyName = QualifiedName.fromDependency(dependency);

        // see if the resolved, direct dependency contain this name.
        // If not, the dependency is declared in a scope that is not used (it was filtered out by the scope filter
        // when the map was created.
        if (rootDependencyMap.getDirectDependencies().containsKey(dependencyName)) {
            // a direct dependency
            final DependencyNode projectDependencyNode = rootDependencyMap.getDirectDependencies().get(dependencyName);
            assert projectDependencyNode != null;

            checkState(visibleScopes.accept(projectDependencyNode, ImmutableList.of()),
                    "Dependency %s maps to %s, but scope filter would exclude it. This should never happen!", dependency, projectDependencyNode);
            computeVersionResolutionForDirectDependency(collector, dependency, projectDependencyNode);
        }

        // could be a dependency in the full dependency list
        final DependencyNode projectDependencyNode = rootDependencyMap.getAllDependencies().get(dependencyName);
        // A project dependency could be e.g. in test scope, but the map has been computed in a narrower scope (e.g. compile)
        // in that case, it does not contain a dependency node for the dependency. That is ok, simply ignore it.
        if (projectDependencyNode == null) {
            return;
        }
        checkState(visibleScopes.accept(projectDependencyNode, ImmutableList.of()),
                "Dependency %s maps to %s, but scope filter would exclude it. This should never happen!", dependency, projectDependencyNode);

        try {
            // remove the test scope for resolving all the transitive dependencies. Anything that was pulled in in test scope,
            // now needs its dependencies resolved in compile+runtime scope, not test scope.
            final ScopeLimitingFilter dependencyScope = ScopeLimitingFilter.computeTransitiveScope(dependency.getScope());
            computeVersionResolutionForTransitiveDependencies(collector, dependency, projectDependencyNode, dependencyScope);
        } catch (ProjectBuildingException e) {
            // This is an optimization and a bug workaround at the same time. Some artifacts exist that
            // specify a packaging that is not natively supported by maven (e.g. bundle of OSGi bundles), however they
            // do not bring the necessary extensions to deal with that type. As a result, this causes a "could not read model"
            // exception. Ignore the transitive dependencies if the project node does not suggest any child artifacts.
            if (projectDependencyNode.getChildren().isEmpty()) {
                LOG.debug("Ignoring model building exception for %s, no children were declared", dependency);
            } else {
                LOG.warn("Could not read POM for %s, ignoring project and its dependencies!", dependency);
            }
        }
    }

    /**
     * Create a version resolution for the given direct requestingDependency and artifact.
     */
    private void computeVersionResolutionForDirectDependency(
            final ImmutableSetMultimap.Builder<QualifiedName, VersionResolution> collector,
            final Dependency requestingDependency,
            final DependencyNode resolvedDependencyNode)
            throws AbstractArtifactResolutionException, VersionRangeResolutionException, MojoExecutionException {
        final QualifiedName requestingDependencyName = QualifiedName.fromDependency(requestingDependency);

        final RepositorySystem repoSystem = context.getRepositorySystem();

        Artifact artifact = convertToPomArtifact(requestingDependency.getArtifact());
        if (artifact.isSnapshot()) {
            // convert version of a snapshot artifact to be SNAPSHOT, otherwise the
            // version range resolver will try to match the timestamp version
            artifact = artifact.setVersion(artifact.getBaseVersion());
        }

        final VersionRangeRequest request = context.createVersionRangeRequest(artifact);
        final VersionRangeResult result = repoSystem.resolveVersionRange(context.getRepositorySystemSession(), request);

        if (!result.getVersions().contains(resolvedDependencyNode.getVersion())) {
            throw new MojoExecutionException(
                    format("Cannot determine the recommended version of dependency '%s'; its version specification is '%s', and the resolved version is '%s'.",
                            requestingDependency, requestingDependency.getArtifact().getBaseVersion(), resolvedDependencyNode.getVersion()));
        }

        // dependency range contains the project version (or matches it)

        // version from the dependency artifact
        final ComparableVersion expectedVersion = getVersion(resolvedDependencyNode);

        // this is a direct dependency; it made it through the filter in resolveProjectDependency.
        final boolean managedDependency = (resolvedDependencyNode.getManagedBits() & DependencyNode.MANAGED_VERSION) != 0;
        final VersionResolution resolution = VersionResolution.forDirectDependency(QualifiedName.fromProject(context.getRootProject()), expectedVersion,
                managedDependency);

        if (isIncluded(resolvedDependencyNode, expectedVersion, expectedVersion)) {
            final Strategy strategy = context.getStrategyCache().forQualifiedName(requestingDependencyName);
            checkState(strategy != null, "Strategy for %s is null, this should never happen (could not find default strategy?", requestingDependencyName);

            if (!strategy.isCompatible(expectedVersion, expectedVersion)) {
                resolution.conflict();
            }
        } else {
            LOG.debug("VersionResolution %s is excluded by configuration.", resolution);
        }

        try {
            collectorLock.lock();
            collector.put(requestingDependencyName, resolution);
        } finally {
            collectorLock.unlock();
        }
    }

    /**
     * Resolve all transitive dependencies relative to a given dependency, based off the artifact given. A scope filter can be added which limits the results to
     * the scopes present in that filter.
     */
    private void computeVersionResolutionForTransitiveDependencies(
            final ImmutableSetMultimap.Builder<QualifiedName, VersionResolution> collector,
            final Dependency requestingDependency,
            final DependencyNode dependencyNodeForDependency,
            final DependencyFilter scopeFilter)
            throws AbstractArtifactResolutionException, ProjectBuildingException, DependencyResolutionException {
        final AndDependencyFilter filter = new AndDependencyFilter(scopeFilter, new CheckExclusionsFilter(requestingDependency.getExclusions()));

        final DependencyMap dependencyMap = new DependencyMapBuilder(context).mapDependency(dependencyNodeForDependency, filter);
        final Collection<DependencyNode> transitiveDependencies = dependencyMap.getAllDependencies().values();
        final QualifiedName requestingDependencyName = QualifiedName.fromDependency(requestingDependency);

        final ImmutableSet<DependencyNode> filteredDependencies = transitiveDependencies.stream()
                .filter(d -> scopeFilter.accept(d, ImmutableList.of()))
                .filter(d -> !d.getDependency().isOptional())
                .collect(toImmutableSet());

        for (final DependencyNode dependencyNode : filteredDependencies) {
            final QualifiedName dependencyName = QualifiedName.fromDependencyNode(dependencyNode);

            final DependencyNode projectDependencyNode = rootDependencyMap.getAllDependencies().get(dependencyName);
            if (projectDependencyNode == null) {
                // the next condition can happen if a dependency is required by one dependency but then overridden by another. e.g.
                //
                //   guava (*29.1-jre*, 25.1-android)
                //    guava 25.1-android depends on org.checkerframework:checker-compat-qual
                //    guava 29.1-jre depends on org.checkerframework:checker-qual
                //
                // as the dependency resolver chose 29.1-jre, only the "checker-qual" dependency will show on the final classpath
                // however, when resolving all dependencies, the code will resolve the dependency which pulls in guava-25.1-android.
                // For that dependency, there will be "checker-compat-qual" in the list of dependencies, but when the code tries to
                // resolve the actual classpath dependency, the "checker-compat-qual" dependency is not in the final classpath.
                //
                // This is normal situation and the dependency can just be dropped.
                //
                continue;
            }

            final ComparableVersion resolvedVersion = getVersion(projectDependencyNode);
            final ComparableVersion expectedVersion = getVersion(dependencyNode);

            final boolean managedDependency = (projectDependencyNode.getManagedBits() & DependencyNode.MANAGED_VERSION) != 0;
            final VersionResolution resolution = VersionResolution.forTransitiveDependency(requestingDependencyName, expectedVersion, managedDependency);

            if (isIncluded(dependencyNode, expectedVersion, resolvedVersion)) {
                final Strategy strategy = context.getStrategyCache().forQualifiedName(dependencyName);
                checkState(strategy != null, "Strategy for %s is null, this should never happen (could not find default strategy?", dependencyName);

                if (!strategy.isCompatible(expectedVersion, resolvedVersion)) {
                    resolution.conflict();
                }
            }

            try {
                collectorLock.lock();
                collector.put(dependencyName, resolution);
            } finally {
                collectorLock.unlock();
            }
        }
    }

    /**
     * Returns true if a given artifact and version should be checked.
     */
    private boolean isIncluded(DependencyNode dependencyNodeForDependency, ComparableVersion expectedVersion, ComparableVersion resolvedVersion) {
        return context.getExclusions().stream().noneMatch(exclusion -> exclusion.matches(dependencyNodeForDependency, expectedVersion, resolvedVersion));
    }

    /**
     * Return a version object for an Artifact.
     */
    private static ComparableVersion getVersion(DependencyNode dependencyNode)
            throws OverConstrainedVersionException {
        checkNotNull(dependencyNode, "dependencyNode is null");

        checkState(dependencyNode.getVersion() != null, "DependencyNode %s has a null version selected. Please report a bug!", dependencyNode);
        return new ComparableVersion(dependencyNode.getVersion().toString());
    }
}
