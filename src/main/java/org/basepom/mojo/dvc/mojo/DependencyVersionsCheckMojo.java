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
package org.basepom.mojo.dvc.mojo;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.basepom.mojo.dvc.AbstractDependencyVersionsMojo;
import org.basepom.mojo.dvc.QualifiedName;
import org.basepom.mojo.dvc.dependency.DependencyMap;
import org.basepom.mojo.dvc.strategy.Strategy;
import org.basepom.mojo.dvc.version.VersionResolutionCollection;
import org.basepom.mojo.dvc.version.VersionResolutionElement;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.version.Version;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableSetMultimap.toImmutableSetMultimap;

/**
 * Resolves all dependencies of a project and reports version conflicts.
 */
@Mojo(name = "check", requiresProject = true, threadSafe = true, defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.NONE)
public class DependencyVersionsCheckMojo
        extends AbstractDependencyVersionsMojo
{
    /**
     * List only dependencies in conflict or all dependencies.
     *
     * @since 3.0.0
     */
    @Parameter(defaultValue = "true", property = "dvc.conflicts-only")
    public boolean conflictsOnly = true;

    /**
     * Fail the build if a conflict is detected. Any conflict (direct and transitive) will cause a failure.
     *
     * @since 3.0.0
     */
    @Parameter(defaultValue = "false", alias = "failBuildInCaseOfConflict", property = "dvc.conflicts-fail-build")
    protected boolean conflictsFailBuild = false;

    /**
     * Fail the build only if a version conflict involves one or more direct dependencies. Direct dependency versions are controlled
     * by the project itself so any conflict here can be fixed by changing the version in the project.
     * <br>
     * It is strongly recommended to review and fix these conflicts.
     *
     * @since 3.0.0
     */
    @Parameter(defaultValue = "false", property = "dvc.direct-conflicts-fail-build")
    protected boolean directConflictsFailBuild = false;

    protected void doExecute(final ImmutableSetMultimap<QualifiedName, VersionResolutionCollection> resolutionMap, final DependencyMap rootDependencyMap)
            throws Exception
    {
        // filter out what to display.
        final ImmutableMap<QualifiedName, Collection<VersionResolutionCollection>> filteredMap = ImmutableMap.copyOf(Maps.filterValues(
                resolutionMap.asMap(),
                v -> {
                    // report if no condition is set.
                    boolean report = true;

                    if (conflictsOnly) {
                        // do not report if conflicts are requested but none exists
                        report &= v.stream().anyMatch(VersionResolutionCollection::hasConflict);
                    }
                    if (directOnly) {
                        // do not report if only directs are requested but it is not direct
                        report &= v.stream().anyMatch(VersionResolutionCollection::hasDirectDependencies);
                    }

                    if (managedOnly) {
                        report &= v.stream().anyMatch(VersionResolutionCollection::hasManagedDependencies);
                    }

                    return report;
                }));

        LOG.report(quiet, "Checking %s%s dependencies%s for '%s' scope%s",
                (directOnly ? "direct" : "all"),
                (managedOnly ? ", managed" : ""),
                (deepScan ? " using deep scan" : ""),
                scope,
                (conflictsOnly ? ", reporting only conflicts" : ""));

        if (filteredMap.isEmpty()) {
            return;
        }

        final ImmutableMap<QualifiedName, DependencyNode> rootDependencies = rootDependencyMap.getAllDependencies();

        boolean directConflicts = false;
        boolean transitiveConflicts = false;

        for (final Map.Entry<QualifiedName, Collection<VersionResolutionCollection>> entry : filteredMap.entrySet()) {
            final ImmutableSetMultimap<ComparableVersion, VersionResolutionCollection> versionMap = entry.getValue().stream()
                    .collect(toImmutableSetMultimap(VersionResolutionCollection::getExpectedVersion, Function.identity()));

            boolean willWarn = false;
            boolean willFail = false;

            final boolean isDirect = entry.getValue().stream().anyMatch(VersionResolutionCollection::hasDirectDependencies);
            final QualifiedName dependencyName = entry.getKey();
            final DependencyNode currentDependency = rootDependencies.get(dependencyName);
            assert currentDependency != null;

            final boolean isManaged = (currentDependency.getManagedBits() & DependencyNode.MANAGED_VERSION) != 0;

            final Version dependencyVersion = currentDependency.getVersion();
            checkState(dependencyVersion != null, "Dependency Version for %s is null! File a bug!", currentDependency);
            final ComparableVersion resolvedVersion = new ComparableVersion(dependencyVersion.toString());

            final Strategy strategy = strategyCache.forQualifiedName(dependencyName);

            final MessageBuilder mb = MessageUtils.buffer();

            mb.strong(dependencyName.getShortName())
                    .a(": ")
                    .strong(resolvedVersion)
                    .format(" (%s%s) - scope: %s - strategy: %s",
                            isDirect ? "direct" : "transitive",
                            isManaged ? ", managed" : "",
                            currentDependency.getDependency().getScope(),
                            strategy.getName()
                    )
                    .newline();

            final int versionPadding = versionMap.keySet().stream().map(v -> v.toString().length()).reduce(0, Math::max);
            for (final Map.Entry<ComparableVersion, Collection<VersionResolutionCollection>> versionEntry : versionMap.asMap().entrySet()) {
                final boolean hasConflictVersion = versionEntry.getValue().stream().anyMatch(VersionResolutionCollection::hasConflict);
                final boolean perfectMatch = versionEntry.getValue().stream().anyMatch(v -> v.isMatchFor(resolvedVersion));
                final String paddedVersion = Strings.padEnd(versionEntry.getKey().toString(), versionPadding + 1, ' ');

                mb.a("       ");

                if (hasConflictVersion) {
                    mb.failure(paddedVersion);
                }
                else if (perfectMatch) {
                    mb.success(paddedVersion);
                }
                else {
                    mb.a(paddedVersion);
                }

                mb.a("expected by ");

                for (Iterator<VersionResolutionElement> it = versionEntry.getValue().stream()
                        .flatMap(v -> v.getRequestingDependencies().stream())
                        .iterator(); it.hasNext(); ) {
                    final VersionResolutionElement versionResolutionElement = it.next();
                    final String name = versionResolutionElement.getRequestingDependency().getShortName();

                    if (versionResolutionElement.isDirectDependency()) {
                        mb.strong("*" + name + "*");
                    }
                    else {
                        mb.a(name);
                    }
                    if (it.hasNext()) {
                        mb.a(", ");
                    }
                }

                mb.newline();

                if (hasConflictVersion) {
                    willWarn = true;
                    willFail |= conflictsFailBuild; // any conflict fails build.
                    willFail |= isDirect && directConflictsFailBuild;

                    directConflicts |= isDirect;       // any direct dependency in conflict
                    transitiveConflicts |= !isDirect;  // any transitive dependency in conflict
                }
            }

            if (willFail) {
                LOG.error("%s", mb);
            }
            else if (willWarn) {
                LOG.warn("%s", mb);
            }
            else {
                LOG.info("%s", mb);
            }
        }

        if (directConflicts && (conflictsFailBuild || directConflictsFailBuild)) {
            throw new MojoFailureException("Version conflict in direct dependencies detected!");
        }

        if (transitiveConflicts && conflictsFailBuild) {
            throw new MojoFailureException("Version conflict in transitive dependencies detected!");
        }
    }
}
