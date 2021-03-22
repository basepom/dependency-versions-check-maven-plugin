## Dependency versions check maven plugin

This plugin verifies that the resolved versions of project
dependencies are mutually compatible to each other.

Apache Maven will resolve all direct and transitive dependencies of a
project and create a tree of dependencies. These dependencies are
chosen based on versions, proximity to the project root and a number
of other factors.

However, **Apache Maven operates under the assumption that every
dependency is always backwards compatible and when two versions are
compared, using the "higher" version will satisfy any dependency.**

As the dependency resolution process is somewhat opaque, this may lead
to situations where a dependency is resolved and a project compiles
successfully but the resulting code does not actually work.

Example with semantically versioned dependencies:

```
project:project
  |
  +--- dependency:dependency_A, version 3.0.0
  |
  +--- dependency:dependency_B
         |
         +--- dependency:dependency_A, version 1.0.0
```

In this scenario, the dependency tree will contain dependency_A 3.0.0
and dependency_B 1.0.0. If the project only uses classes from the
3.0.0 version of dependency_A and dependency_B, the code will compile
just fine. If any of these classes from dependency_B now uses classes
that are present in version 1.0.0 of dependency_A but not in the 3.0.0
version (semantic versioning allows for 1.0.0 and 3.0.0 to be neither
forward nor backward compatible). In this scenario, there is no
version of dependency_A that will allow the code to actually
work. However Maven neither warns nor fails the build.

The dependency version check (dvc) plugin can detect these problems
and warn or fail a build accordingly, thus capturing these problems at
build time. It provides a much richer mechanism for version comparison
and while it can not modify or change the dependency resolution
directly, it can highlight problems and if necessary fail a build to
allow manual analysis and if necessary changes to the set of
dependencies to ensure that all provides dependencies are actually
compatible.



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
[ERROR] dependency:dependency_A: 3.0.0 (direct) - scope: compile - strategy: apr
       1.0.0 expected by dependency:dependency_B
       3.0.0 expected by *project:project*
```

Such an error may be used to fail the build.

---

This plugin is a rewrite of the original [Ning dependency version check plugin](https://github.com/ning/maven-dependency-versions-check-plugin).
