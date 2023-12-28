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

import static com.google.common.base.Preconditions.checkState;

import org.basepom.mojo.dvc.AbstractDependencyVersionsMojo;
import org.basepom.mojo.dvc.QualifiedName;
import org.basepom.mojo.dvc.dependency.DependencyMap;
import org.basepom.mojo.dvc.version.VersionResolutionCollection;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.version.Version;

/**
 * Resolves all dependencies of a project and lists all versions for any artifact and what version was chosen by the resolver.
 *
 * @since 2.0.0
 */
@Mojo(name = "list", requiresProject = true, threadSafe = true, requiresDependencyResolution = ResolutionScope.NONE, defaultPhase = LifecyclePhase.VERIFY)
public final class DependencyVersionsListMojo
        extends AbstractDependencyVersionsMojo {

    /**
     * List only dependencies in conflict or all dependencies.
     */
    @Parameter(defaultValue = "false", property = "dvc.conflicts-only")
    public boolean conflictsOnly = false;

    @Override
    protected void doExecute(final ImmutableSetMultimap<QualifiedName, VersionResolutionCollection> resolutionMap, final DependencyMap rootDependencyMap) {
        final var filteredMap = ImmutableMap.copyOf(Maps.filterValues(
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

        log.report(quiet, "%s%s dependencies%s for '%s' scope%s:",
                (directOnly ? "Direct" : "All"),
                (managedOnly ? " managed" : ""),
                (deepScan ? " using deep scan" : ""),
                scope,
                (conflictsOnly ? ", reporting only conflicts" : ""));

        if (filteredMap.isEmpty()) {
            return;
        }

        final var rootDependencies = rootDependencyMap.getAllDependencies();

        // calculate padding for columnar output
        final int namePadding = filteredMap.keySet().stream()
                .map(QualifiedName::length).reduce(0, Math::max);
        final int scopePadding = rootDependencies.entrySet().stream()
                .filter(e -> filteredMap.containsKey(e.getKey()))
                .map(e -> e.getValue().getDependency().getScope().length()).reduce(0, Math::max);

        for (final var entry : filteredMap.entrySet()) {
            final QualifiedName dependencyName = entry.getKey();
            final DependencyNode currentDependency = rootDependencies.get(dependencyName);
            assert currentDependency != null;

            final ImmutableSortedSet<VersionResolutionCollection> resolutions = ImmutableSortedSet.copyOf(entry.getValue());
            final MessageBuilder mb = MessageUtils.buffer();

            mb.a(Strings.padEnd(dependencyName.getShortName() + ": ", namePadding + 2, ' '))
                    .a(Strings.padEnd(currentDependency.getDependency().getScope(), scopePadding + 1, ' '));

            // fetch the resolved version from the dependency in the tree.
            final Version dependencyVersion = currentDependency.getVersion();
            checkState(dependencyVersion != null, "Dependency Version for %s is null! File a bug!", currentDependency);
            final ComparableVersion actualVersion = new ComparableVersion(dependencyVersion.toString());

            final boolean isDirect = resolutions.stream().anyMatch(VersionResolutionCollection::hasDirectDependencies);
            final boolean isManaged = (currentDependency.getManagedBits() & DependencyNode.MANAGED_VERSION) != 0;

            if (isManaged) {
                mb.warning(actualVersion);
            } else if (isDirect) {
                mb.strong(actualVersion);
            } else {
                mb.a(actualVersion);
            }

            final var unselectedVersions = ImmutableSortedSet.copyOf(Sets.filter(resolutions, v -> !v.isMatchFor(actualVersion)));

            if (!unselectedVersions.isEmpty()) {
                mb.a(" (");

                for (final var it = unselectedVersions.iterator(); it.hasNext(); ) {
                    final VersionResolutionCollection resolution = it.next();

                    final String result = resolution.getExpectedVersion().toString();

                    if (resolution.hasConflict()) {
                        mb.failure("!" + result + "!");
                    } else if (resolution.isMatchFor(actualVersion)) {
                        mb.success(result);
                    } else if (resolution.hasDirectDependencies()) {
                        mb.strong("*" + result + "*");
                    } else {
                        mb.a(result);
                    }
                    if (it.hasNext()) {
                        mb.a(", ");
                    }
                }

                mb.a(")");
            }

            log.info("%s", mb);
        }
    }
}

