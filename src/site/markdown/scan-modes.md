## Scan Modes

The plugin supports two different scan modes: normal scan and deep scan.

### Normal scan

In this mode, the plugin will resolve the dependency tree for every
direct dependency of a project. It will then compare the versions with
the versions defined in the project POM and the dependency tree
resolved by maven itself

A direct dependency is a dependency that is listed in the POM
itself. E.g the dependency versions check plugin has the following
direct dependencies:

```
Direct dependencies for 'test' scope:
com.google.guava:guava:                                 compile  29.0-jre (25.1-android)
org.apache.maven.plugin-tools:maven-plugin-annotations: provided 3.6.0
org.apache.maven.resolver:maven-resolver-api:           provided 1.4.2 (1.4.1)
org.apache.maven.resolver:maven-resolver-util:          compile  1.4.2 (1.4.1)
org.apache.maven.shared:maven-shared-utils:             compile  3.3.3 (3.2.1)
org.apache.maven:maven-artifact:                        provided 3.6.3 (3.0)
org.apache.maven:maven-core:                            provided 3.6.3
org.apache.maven:maven-model:                           provided 3.6.3
org.apache.maven:maven-plugin-api:                      provided 3.6.3
org.codehaus.plexus:plexus-component-annotations:       provided 2.1.0
org.junit.jupiter:junit-jupiter-api:                    test     5.7.0
org.junit.jupiter:junit-jupiter-engine:                 test     5.7.0
org.slf4j:slf4j-api:                                    provided 1.7.30 (1.7.29)
```

Each of these direct dependencies has a sub-tree of dependencies:

```
project
  |
  +--- dependency A --- dependency D
  +--- dependency B -+- dependency E
  |                  +- dependency F
  +--- dependency C
```

The plugin will compute the dependency tree starting at `dependency A`,
`dependency B` and `dependency C` and then compare the versions
that the different dependencies have in each dependency tree.

It will also limit its scope to the dependencies that actually end up
on the class path for the selected scope.

This mode assumes that all dependencies used have valid dependency
sub-trees. The scan cares about that all these subtrees can be
combined and the versions chosen by the maven dependency resolution
satisfy the requirements of all the different dependencies.

The normal scan mode is sufficient for almost all projects. It should
detect most real problems that stem from the combination of
dependencies in the current project.

Example for the "normal" scan mode (for the plugin itself)

```
All dependencies for 'test' scope:
aopalliance:aopalliance:                                provided 1.0
com.google.code.findbugs:jsr305:                        compile  3.0.2
com.google.errorprone:error_prone_annotations:          compile  2.4.0 (2.1.3, 2.3.4)
com.google.guava:failureaccess:                         compile  1.0.1
com.google.guava:guava:                                 compile  29.0-jre (25.1-android)
com.google.guava:listenablefuture:                      compile  9999.0-empty-to-avoid-conflict-with-guava
com.google.inject:guice (no_aop):                       provided 4.2.1
com.google.j2objc:j2objc-annotations:                   compile  1.3 (1.1)
commons-io:commons-io:                                  compile  2.6 (2.5)
javax.annotation:jsr250-api:                            provided 1.0
javax.enterprise:cdi-api:                               provided 1.0
javax.inject:javax.inject:                              provided 1
org.apache.commons:commons-lang3:                       provided 3.8.1
org.apache.maven.plugin-tools:maven-plugin-annotations: provided 3.6.0
org.apache.maven.resolver:maven-resolver-api:           provided 1.4.2 (1.4.1)
org.apache.maven.resolver:maven-resolver-impl:          provided 1.4.1
org.apache.maven.resolver:maven-resolver-spi:           provided 1.4.1
org.apache.maven.resolver:maven-resolver-util:          compile  1.4.2 (1.4.1)
org.apache.maven.shared:maven-shared-utils:             compile  3.3.3 (3.2.1)
org.apache.maven:maven-artifact:                        provided 3.6.3 (3.0)
org.apache.maven:maven-builder-support:                 provided 3.6.3
org.apache.maven:maven-core:                            provided 3.6.3
org.apache.maven:maven-model:                           provided 3.6.3
org.apache.maven:maven-model-builder:                   provided 3.6.3
org.apache.maven:maven-plugin-api:                      provided 3.6.3
org.apache.maven:maven-repository-metadata:             provided 3.6.3
org.apache.maven:maven-resolver-provider:               provided 3.6.3
org.apache.maven:maven-settings:                        provided 3.6.3
org.apache.maven:maven-settings-builder:                provided 3.6.3
org.apiguardian:apiguardian-api:                        test     1.1.0
org.checkerframework:checker-qual:                      compile  2.11.1
org.codehaus.plexus:plexus-classworlds:                 provided 2.6.0
org.codehaus.plexus:plexus-component-annotations:       provided 2.1.0
org.codehaus.plexus:plexus-interpolation:               provided 1.25
org.codehaus.plexus:plexus-utils:                       provided 3.2.1 (3.0.10, 3.0.20)
org.eclipse.sisu:org.eclipse.sisu.inject:               provided 0.3.4
org.eclipse.sisu:org.eclipse.sisu.plexus:               provided 0.3.4
org.junit.jupiter:junit-jupiter-api:                    test     5.7.0
org.junit.jupiter:junit-jupiter-engine:                 test     5.7.0
org.junit.platform:junit-platform-commons:              test     1.7.0
org.junit.platform:junit-platform-engine:               test     1.7.0
org.opentest4j:opentest4j:                              test     1.2.0
org.slf4j:slf4j-api:                                    provided 1.7.30 (1.7.29)
org.sonatype.plexus:plexus-cipher:                      provided 1.7
org.sonatype.plexus:plexus-sec-dispatcher:              provided 1.4
```

In this output, there are more dependencies (each of the direct dependencies may have transitive dependencies). The result are all dependencies that end up on the class path, both direct and transitive dependencies.

### Deep scan

The deep scan mode differs that it not only resolves the dependencies for the project that are directly referenced in the project POM but everything that is present  on the classpath in the selected scope.

```
All dependencies using deep scan for 'test' scope:
aopalliance:aopalliance:                                provided 1.0
com.google.code.findbugs:jsr305:                        compile  3.0.2 (3.0.1)
com.google.errorprone:error_prone_annotations:          compile  2.4.0 (2.1.3, 2.3.4)
com.google.guava:failureaccess:                         compile  1.0.1
com.google.guava:guava:                                 compile  29.0-jre (25.1-android)
com.google.guava:listenablefuture:                      compile  9999.0-empty-to-avoid-conflict-with-guava
com.google.inject:guice (no_aop):                       provided 4.2.1
com.google.j2objc:j2objc-annotations:                   compile  1.3 (1.1)
commons-io:commons-io:                                  compile  2.6 (2.5)
javax.annotation:jsr250-api:                            provided 1.0
javax.enterprise:cdi-api:                               provided 1.0
javax.inject:javax.inject:                              provided 1
org.apache.commons:commons-lang3:                       provided 3.8.1
org.apache.maven.plugin-tools:maven-plugin-annotations: provided 3.6.0
org.apache.maven.resolver:maven-resolver-api:           provided 1.4.2 (1.4.1)
org.apache.maven.resolver:maven-resolver-impl:          provided 1.4.1
org.apache.maven.resolver:maven-resolver-spi:           provided 1.4.1
org.apache.maven.resolver:maven-resolver-util:          compile  1.4.2 (1.4.1)
org.apache.maven.shared:maven-shared-utils:             compile  3.3.3 (3.2.1)
org.apache.maven:maven-artifact:                        provided 3.6.3 (3.0)
org.apache.maven:maven-builder-support:                 provided 3.6.3
org.apache.maven:maven-core:                            provided 3.6.3
org.apache.maven:maven-model:                           provided 3.6.3
org.apache.maven:maven-model-builder:                   provided 3.6.3
org.apache.maven:maven-plugin-api:                      provided 3.6.3
org.apache.maven:maven-repository-metadata:             provided 3.6.3
org.apache.maven:maven-resolver-provider:               provided 3.6.3
org.apache.maven:maven-settings:                        provided 3.6.3
org.apache.maven:maven-settings-builder:                provided 3.6.3
org.apiguardian:apiguardian-api:                        test     1.1.0
org.checkerframework:checker-qual:                      compile  2.11.1
org.codehaus.plexus:plexus-classworlds:                 provided 2.6.0 (2.5.2)
org.codehaus.plexus:plexus-component-annotations:       provided 2.1.0 (1.5.5)
org.codehaus.plexus:plexus-interpolation:               provided 1.25
org.codehaus.plexus:plexus-utils:                       provided 3.2.1 (1.5.5, 3.0.10, 3.0.17, 3.0.20)
org.eclipse.sisu:org.eclipse.sisu.inject:               provided 0.3.4
org.eclipse.sisu:org.eclipse.sisu.plexus:               provided 0.3.4
org.junit.jupiter:junit-jupiter-api:                    test     5.7.0
org.junit.jupiter:junit-jupiter-engine:                 test     5.7.0
org.junit.platform:junit-platform-commons:              test     1.7.0
org.junit.platform:junit-platform-engine:               test     1.7.0
org.opentest4j:opentest4j:                              test     1.2.0
org.slf4j:slf4j-api:                                    provided 1.7.30 (1.7.25, 1.7.29)
org.sonatype.plexus:plexus-cipher:                      provided 1.7 (1.4)
org.sonatype.plexus:plexus-sec-dispatcher:              provided 1.4
```

While the output still has the same number of dependencies, there are now more versions listed, e.g. `3.2.1 (1.5.5, 3.0.10, 3.0.17, 3.0.20)` for `plexus-utils` while the regular scan only detected `3.2.1 (3.0.10, 3.0.20)`. Deep scan detected the same dependency is referenced transitively from other transitive dependencies with these additional versions.

Deep scan is most useful to detect very obscure dependency problems that often manifest in `NoClassDefFoundError` or `IncompatibleClassChangeError` problems.

This mode needs to load a very large number of dependencies and an initial run can (especially for a large project) take a long time and load a lot of data from the network.
