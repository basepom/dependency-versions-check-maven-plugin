= Dependency versions check maven plugin

== Introduction

This plugin verifies that the resolved versions of project
dependencies are mutually compatible to each other.

Apache Maven will resolve all direct and transitive dependencies of a
project and create a tree of dependencies. These dependencies are
chosen based on versions, proximity to the project root and a number
of other factors.

However, Apache Maven operates under the assumption that every
dependency is always backwards compatible and when two versions are
compared, using the "higher" version will satisfy any dependency.

As the dependency resolution process is somewhat opaque, this may lead
to situations where a dependency is resolved and a project compiles
successfully but the resulting code does not actually work.

Example with semantically versioned dependencies:

```
project
  |
  +----- dependency A, version 3.0.0
  |
  +----- dependency B
            |
            +------- dependency A, version 1.0.0
```

In this scenario, the dependency tree will contain dependency A 3.0.0
and dependency B 1.0.0. If the project only uses classes from the
3.0.0 version of dependency A and dependency B, the code will compile
just fine. If any of these classes from dependency B now uses classes
that are present in version 1.0.0 of dependency A but not in the 3.0.0
version (semantic versioning allows for 1.0.0 and 3.0.0 to be neither
forward nor backward compatible). In this scenario, there is no
version of dependency A that will allow the code to actually
work. However Maven neither warns nor fails the build.

The dependency version check (dvc) plugin can detect these problems
and warn or fail a build accordingly, thus capturing these problems at
build time.

In the scenario above, adding

``` xml
<plugin>
    <groupId>org.basepom.maven</groupId>
    <artifactId>dependency-versions-check-maven-plugin</artifactId>
    <configuration>
        <directConflictFailsBuild>true</directConflictFailsBuild>
        <resolvers>
            <resolver>
                <strategy>apr</strategy>
                <includes>
                    <include>dependency:dependencyA</include>
                </includes>
            </resolver>
        </resolvers>
    </configuration>
</plugin>
```

would result in:

```
[ERROR] dependency:dependencyA: 3.0.0 (direct) - scope: compile - strategy: apr
       1.0.0 expected by dependency:dependencyB
       3.0.0 expected by *project:project*
```

and fail the build.


== Maven Goals

=== dependency-versions-check:list - display project dependencies

This goal scans the dependency tree and displays all the dependencies of a project and the versions detected in the dependency tree.

==== Supported Options (see below for details)

* `<skip>...</skip>` (property: ${dvc.skip}) (default: false)

Skip plugin execution.


* `<conflictOnly> ... </conflictOnly>` (property: `${dvc.conflict-only}`) (default: false)

If true, only list dependencies that are in conflict.











For example, for this project the set of dependencies looks like this (this is the output of the Maven dependency plugin):

```
aopalliance:aopalliance:jar:1.0:provided
com.google.code.findbugs:jsr305:jar:3.0.2:compile
com.google.errorprone:error_prone_annotations:jar:2.4.0:compile
com.google.guava:failureaccess:jar:1.0.1:compile
com.google.guava:guava:jar:29.0-jre:compile
com.google.guava:listenablefuture:jar:9999.0-empty-to-avoid-conflict-with-guava:compile
com.google.inject:guice:jar:no_aop:4.2.1:provided
com.google.j2objc:j2objc-annotations:jar:1.3:compile
commons-io:commons-io:jar:2.6:compile
javax.annotation:jsr250-api:jar:1.0:provided
javax.enterprise:cdi-api:jar:1.0:provided
javax.inject:javax.inject:jar:1:provided
org.apache.commons:commons-lang3:jar:3.8.1:provided
org.apache.maven.plugin-tools:maven-plugin-annotations:jar:3.6.0:provided
org.apache.maven.resolver:maven-resolver-api:jar:1.4.2:provided
org.apache.maven.resolver:maven-resolver-impl:jar:1.4.1:provided
org.apache.maven.resolver:maven-resolver-spi:jar:1.4.1:provided
org.apache.maven.resolver:maven-resolver-util:jar:1.4.2:compile
org.apache.maven.shared:maven-shared-utils:jar:3.3.3:compile
org.apache.maven:maven-artifact:jar:3.6.3:provided
org.apache.maven:maven-builder-support:jar:3.6.3:provided
org.apache.maven:maven-core:jar:3.6.3:provided
org.apache.maven:maven-model-builder:jar:3.6.3:provided
org.apache.maven:maven-model:jar:3.6.3:provided
org.apache.maven:maven-plugin-api:jar:3.6.3:provided
org.apache.maven:maven-repository-metadata:jar:3.6.3:provided
org.apache.maven:maven-resolver-provider:jar:3.6.3:provided
org.apache.maven:maven-settings-builder:jar:3.6.3:provided
org.apache.maven:maven-settings:jar:3.6.3:provided
org.apiguardian:apiguardian-api:jar:1.1.0:test
org.checkerframework:checker-qual:jar:2.11.1:compile
org.codehaus.plexus:plexus-classworlds:jar:2.6.0:provided
org.codehaus.plexus:plexus-component-annotations:jar:2.1.0:provided
org.codehaus.plexus:plexus-interpolation:jar:1.25:provided
org.codehaus.plexus:plexus-utils:jar:3.2.1:provided
org.eclipse.sisu:org.eclipse.sisu.inject:jar:0.3.4:provided
org.eclipse.sisu:org.eclipse.sisu.plexus:jar:0.3.4:provided
org.junit.jupiter:junit-jupiter-api:jar:5.7.0:test
org.junit.jupiter:junit-jupiter-engine:jar:5.7.0:test
org.junit.platform:junit-platform-commons:jar:1.7.0:test
org.junit.platform:junit-platform-engine:jar:1.7.0:test
org.opentest4j:opentest4j:jar:1.2.0:test
org.slf4j:slf4j-api:jar:1.7.30:provided
org.sonatype.plexus:plexus-cipher:jar:1.4:provided
org.sonatype.plexus:plexus-sec-dispatcher:jar:1.4:provided
```

It is not clear from this list, why this version was chosen and (if a given dependencies was present with different versions), what versions were superseded.
