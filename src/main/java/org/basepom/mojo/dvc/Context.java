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
     * @return True if any unresolved system artifacts should fail the build.
     */
    boolean isUnresolvedSystemArtifactsFailBuild();

    /**
     * @return True if the resolver should use multiple threads.
     */
    boolean useFastResolution();

    /**
     * @return True if a deep scan should be performed instead of regular scan.
     */
    boolean useDeepScan();

    /**
     * @return All configured exclusions.
     */
    List<VersionCheckExcludes> getExclusions();

    /**
     * @return The lookup cache for the Strategy resolution
     */
    StrategyCache getStrategyCache();

    /**
     * @return The Maven project builder.
     */
    ProjectBuilder getProjectBuilder();

    /**
     * @return The Maven project dependency resolver.
     */
    ProjectDependenciesResolver getProjectDependenciesResolver();

    /**
     * @return The root project.
     */
    MavenProject getRootProject();

    /**
     * @return All projects that are in the current reactor.
     */
    List<MavenProject> getReactorProjects();

    /**
     * @return The repository session
     */
    RepositorySystemSession getRepositorySystemSession();

    /**
     * @return The repository system for dependency resolution.
     */
    RepositorySystem getRepositorySystem();

    /**
     * @return A new project building request.
     */
    ProjectBuildingRequest createProjectBuildingRequest();

    /**
     * @param artifact The artifact to define the version range resolution request.
     *
     * @return A version range resolution request.
     */
    VersionRangeRequest createVersionRangeRequest(Artifact artifact);
}
