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

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.basepom.mojo.dvc.model.VersionCheckExcludes;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.VersionRangeRequest;

import java.util.List;

public interface Context
{
    /**
     * fail if any system artifacts can not be resolved.
     */
    boolean isUnresolvedSystemArtifactsFailBuild();

    /**
     * True if the resolver should use multiple threads.
     */
    boolean useFastResolution();

    /**
     * Run deep scan instead of regular scan.
     */
    boolean useDeepScan();

    /**
     * Get all configured exclusions.
     */
    List<VersionCheckExcludes> getExclusions();

    /**
     * Lookup cache for the Strategy resolution
     */
    StrategyCache getStrategyCache();

    /**
     * Return the Maven project builder.
     */
    ProjectBuilder getProjectBuilder();

    /**
     * Return the Maven project dependency resolver.
     */
    ProjectDependenciesResolver getProjectDependenciesResolver();

    /**
     * Returns the root project.
     */
    MavenProject getRootProject();

    /**
     * Return all projects that are in the current reactor.
     */
    List<MavenProject> getReactorProjects();

    /**
     * Repository session
     */
    RepositorySystemSession getRepositorySystemSession();

    /**
     * Repository system for dependency resolution.
     */
    RepositorySystem getRepositorySystem();

    /**
     * Create a new project building request.
     */
    ProjectBuildingRequest createProjectBuildingRequest();

    /**
     * Create a version range resolution request.
     */
    VersionRangeRequest createVersionRangeRequest(Artifact artifact);

}
