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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.basepom.mojo.dvc.dependency.DependencyMap;
import org.basepom.mojo.dvc.dependency.DependencyMapBuilder;
import org.basepom.mojo.dvc.dependency.DependencyTreeResolver;
import org.basepom.mojo.dvc.model.ResolverDefinition;
import org.basepom.mojo.dvc.model.VersionCheckExcludes;
import org.basepom.mojo.dvc.strategy.StrategyProvider;
import org.basepom.mojo.dvc.version.VersionResolutionCollection;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.util.artifact.JavaScopes;

import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Base code for all the mojos. Contains the dependency resolvers and the common options.
 */
public abstract class AbstractDependencyVersionsMojo
        extends AbstractMojo
        implements Context
{
    private static final ImmutableSet<String> VALID_SCOPES = ImmutableSet.of(
            ScopeLimitingFilter.COMPILE_PLUS_RUNTIME,
            JavaScopes.COMPILE,
            JavaScopes.RUNTIME,
            JavaScopes.TEST);

    protected final PluginLog LOG = new PluginLog(this.getClass());

    @Parameter(defaultValue = "${project}", readonly = true)
    public MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    public MavenSession mavenSession;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    public List<MavenProject> reactorProjects;

    @Component
    public ProjectBuilder mavenProjectBuilder;

    @Component
    public ProjectDependenciesResolver projectDependenciesResolver;

    @Component
    public RepositorySystem repositorySystem;

    /**
     * The strategy provider. This can be requested by other pieces to add
     * additional strategies.
     */
    @Component
    public StrategyProvider strategyProvider;

    /**
     * List of elements that will be removed from the version
     * check. This allows potential conflicts to be excluded. Each
     * element consists of a dependency pattern [groupId]:[artifactId]
     * with supported wildcards and an expected version (which is the
     * version in the dependency tree) and a resolved version (the
     * version that is used after dependency resolution). Any match in
     * this list will be excluded from the dependency version check.
     */
    @Parameter(alias = "exceptions")
    public VersionCheckExcludes[] exclusions = new VersionCheckExcludes[0];

    /**
     * Skip the plugin execution.
     */
    @Parameter(defaultValue = "false", property = "dvc.skip")
    public boolean skip = false;

    /**
     * Include POM projects when running on a multi-module project.
     *
     * @since 3.0.0
     */
    @Parameter(defaultValue = "false", property = "dvc.includePomProjects")
    public boolean includePomProjects = false;

    /**
     * Silence all non-output and non-error messages.
     *
     * @since 3.0.0
     */
    @Parameter(defaultValue = "false", property = "dvc.quiet")
    public boolean quiet = false;

    /**
     * The scope to list. Defaults to "compile". Valid values are "compile+runtime", "compile", "test" and "runtime".
     */
    @Parameter(defaultValue = "test", property = "scope")
    public String scope = JavaScopes.TEST;

    /**
     * Use deep scan or regular scan. Deep scan looks at all dependencies in the dependency tree, while
     * regular scan only looks one level deep into the direct dependencies.
     */
    @Parameter(defaultValue = "false", property = "dvc.deep-scan")
    public boolean deepScan = false;

    /**
     * Whether to list only direct dependencies or all dependencies. Default is to list all dependencies.
     */
    @Parameter(defaultValue = "false", property = "dvc.direct-only")
    public boolean directOnly = false;

    /**
     * Whether to list all dependencies or only managed dependencies. Default is to list all dependencies.
     */
    @Parameter(defaultValue = "false", property = "dvc.managed-only")
    public boolean managedOnly = false;

    /**
     * Run dependency resolution with multiple threads. Should only ever set to false if
     * the plugin shows stability problems when resolving dependencies. Please file a bug in that case, too.
     */
    @Parameter(defaultValue = "true", property = "dvc.fast-resolution")
    public boolean fastResolution = true;

    /**
     * Fail the build if an artifact in system scope can not be resolved. Those are notoriously dependent on
     * the local build environment and some outright fail (e.g. referencing the tools.jar in a JDK8+ environment).
     * <p>
     * Setting this flag to true will fail the build if any system artifact in the transitive set of dependencies
     * can not be resolved. This is almost never what is wanted, except when building a project with a direct system
     * dependency.
     *
     * @since 3.0.0
     */
    @Parameter(defaultValue = "false", property = "dvc.unresolved-system-artifacts-fail-build")
    protected boolean unresolvedSystemArtifactsFailBuild = false;

    /**
     * List of resolvers to apply specific strategies to dependencies. A resolver contains of a strategy name
     * and a list of includes. The include syntax is [group-id]:[artifact-id] where each pattern segment
     * supports full and partial wildcards ("*").
     * <p>
     * The plugin includes some default strategies: apr, default, single-digit and two-digits-backward-compatible.
     * Additional strategies can be defined in code and added to the plugin classpath.
     */
    @Parameter
    public ResolverDefinition[] resolvers = new ResolverDefinition[0];

    /**
     * Sets the default strategy to use to evaluate whether two dependency versions are compatible or not.
     * The default strategy matches the Maven dependency resolution itself; any two dependencies that maven considers
     * compatible will be accepted by the default strategy.
     */
    @Parameter(defaultValue = "default", property = "dependency-versions-check.default")
    public String defaultStrategy = "default";

    protected StrategyCache strategyCache;

    public void execute()
            throws MojoExecutionException, MojoFailureException
    {
        try {
            for (VersionCheckExcludes exclusion : exclusions) {
                checkState(exclusion.isValid(), "Invalid exclusion specification: '%s'", exclusion);
            }

            checkState(!Strings.nullToEmpty(scope).trim().isEmpty() && VALID_SCOPES.contains(scope), "Scope '%s' is invalid", scope);

            if (skip) {
                LOG.report(quiet, "Skipping plugin execution");
                return;
            }

            if (!includePomProjects && "pom".equals(project.getPackaging())) {
                LOG.report(quiet, "Ignoring POM project");
                return;
            }

            LOG.debug("Starting %s mojo run!", this.getClass().getSimpleName());

            this.strategyCache = new StrategyCache(strategyProvider, resolvers, defaultStrategy);

            final ScopeLimitingFilter scopeFilter = createScopeFilter();
            final DependencyMap rootDependencyMap = new DependencyMapBuilder(this).mapProject(project, scopeFilter);

            try (DependencyTreeResolver dependencyTreeResolver = new DependencyTreeResolver(this, rootDependencyMap)) {
                final ImmutableSetMultimap<QualifiedName, VersionResolutionCollection> resolutionMap = dependencyTreeResolver.computeResolutionMap(project, scopeFilter);
                doExecute(resolutionMap, rootDependencyMap);
            }
        }
        catch (MojoExecutionException | MojoFailureException e) {
            throw e;
        }
        catch (Exception e) {
            throw new MojoExecutionException("While running mojo: ", e);
        }
        finally {
            LOG.debug("Ended %s mojo run!", this.getClass().getSimpleName());
        }
    }

    /**
     * Subclasses need to implement this method.
     */
    protected abstract void doExecute(ImmutableSetMultimap<QualifiedName, VersionResolutionCollection> resolutionMap, DependencyMap rootDependencyMap)
            throws Exception;

    /**
     * Defines the scope used to resolve the project dependencies. The project dependencies will be limited to the dependencies that match this
     * filter. The list mojo overrides this to limit the scope in which dependencies are listed. By default, include everything.
     */
    protected ScopeLimitingFilter createScopeFilter()
    {
        return ScopeLimitingFilter.computeDependencyScope(scope);
    }

    @Override
    public boolean useFastResolution()
    {
        return fastResolution;
    }

    @Override
    public boolean useDeepScan()
    {
        return deepScan;
    }

    @Override
    public StrategyCache getStrategyCache()
    {
        return strategyCache;
    }

    @Override
    public ProjectBuilder getProjectBuilder()
    {
        return mavenProjectBuilder;
    }

    @Override
    public ProjectDependenciesResolver getProjectDependenciesResolver()
    {
        return projectDependenciesResolver;
    }

    @Override
    public MavenProject getRootProject()
    {
        return project;
    }

    @Override
    public List<MavenProject> getReactorProjects()
    {
        return ImmutableList.copyOf(reactorProjects);
    }

    @Override
    public RepositorySystemSession getRepositorySystemSession()
    {
        return mavenSession.getRepositorySession();
    }

    @Override
    public RepositorySystem getRepositorySystem() { return repositorySystem; }

    @Override
    public ProjectBuildingRequest createProjectBuildingRequest()
    {
        DefaultProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(mavenSession.getProjectBuildingRequest());
        buildingRequest.setRemoteRepositories(project.getRemoteArtifactRepositories());
        return buildingRequest;
    }

    @Override
    public VersionRangeRequest createVersionRangeRequest(Artifact artifact)
    {
        checkNotNull(artifact, "artifact is null");
        return new VersionRangeRequest(artifact, project.getRemoteProjectRepositories(), "");
    }

    @Override
    public List<VersionCheckExcludes> getExclusions()
    {
        return Arrays.asList(exclusions);
    }

    @Override
    public boolean isUnresolvedSystemArtifactsFailBuild()
    {
        return unresolvedSystemArtifactsFailBuild;
    }
}
