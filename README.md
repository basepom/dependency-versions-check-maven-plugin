# Dependency versions check maven plugin

## Introduction

This plugin verifies that the resolved versions of project
dependencies are mutually compatible to each other.

This README only serves as a quick overview of the plugin. Please see the [Documentation Site](https://basepom.github.io/dependency-versions-check-maven-plugin/) for a full overview of the plugin and its function.

## Cheat Sheet

* the `list` goal lists all dependencies and their final resolved versions
* the `check` goal verifies that all resolved dependency versions match the project requirements

The `list` goal is usually run interactively while the `check` goal should be run as part of a build.

### Configuration

```xml
<configuration>
    <skip>...</skip>
    <includePomProjects>...</includePomProjects>
    <quiet>...</quiet>
    <scope>...</scope>
    <deepScan>...</deepScan>
    <directOnly>...</directOnly>
    <managedOnly>...</managedOnly>
    <fastResolution>...</fastResolution>
    <unresolvedSystemArtifactsFailBuild>...</unresolvedSystemArtifactsFailBuild>
    <defaultStrategy>...</defaultStrategy>
    <conflictsOnly>...</conflictsOnly>
    <conflictsFailBuild>...</conflictsFailBuild>
    <directConflictsFailBuild>...</directConflictsFailBuild>
    <exceptions>
        <exception>
            <dependency>...</dependency>
            <expected>...</expected>
            <resolved>...</resolved>
        </exception>
        <exception>
            ...
        </exception>
    </exceptions>
    <resolvers>
        <resolver>
            <strategy>...</strategy>
            <includes>
                <include>...</include>
                ...
            </includes>
        </resolver>
        <resolver>
            ...
        </resolver>
    </resolvers>
</configuration>
```


configuration key  | function | type | command line | default
-----------------  | -------- | ---- | ------------ | -------
`skip` [*L*, *C*] | skip plugin execution  | boolean | `dvc.skip` | `false`
`includePomProjects` [*L*, *C*] | also process pom projects | boolean |  `dvc.include-pom-projects` | `false`
`quiet` [*L*, *C*]| suppress non-essential output | boolean |  `dvc.quiet` | `false`
`scope` [*L*, *C*]| select the scope to use for artifact resolution | one of `compile`, `runtime`, `test`, `compile+runtime` |  `dvc.scope` | `test`
`deepScan` [*L*, *C*] | resolve all artifacts, not just direct | boolean |  `dvc.deep-scan` | `false`
`directOnly` [*L*, *C*] | check only direct dependencies | boolean | `dvc.direct-only` | `false`
`managedOnly` [*L*, *C*] | check only managed dependencies | boolean | `dvc.managed-only` | `false`
`fastResolution` [*L*, *C*] | use parallel dependency resolution | boolean | `dvc.fast-resolution` | `true`
`unresolvedSystemArtifactsFailBuild` [*L*, *C*] | `system` scope artifacts that can not be resolved will fail the build | boolean |  `dvc.unresolved-system-artifacts-fail-build` | `false`
`defaultStrategy` [*L*, *C*] | default artifact matching strategy | string | `dvc.default-strategy` | `default`
`conflictsOnly` [*L*, *C*] | only report dependencies in conflict | boolean |  `dvc.conflicts-only` | `true` for `check` goal, `false` for `list` goal
`conflictsFailBuild` / `failBuildInCaseOfConflict` [C] | any version conflict will fail the build | boolean |  `dvc.conflicts-fail-build` | `false`
`directConflictsFailBuild` [*C*] | any conflict in a direct dependency will fail the build | boolean |  `dvc.direct-conflicts-fail-build` | `false`
`exceptions` [*L*, *C*] | set of exceptions influencing the version resolution | set of exceptions | - | -
`resolvers` [*L*, *C*] | resolver strategies for specific dependencies | set of resolvers | - | -

(*L* = `list` goal, *C* = `check` goal)


### Exceptions

An exception defines an acceptable conflict which would otherwise fail the build:

```xml
<exceptions>
    <exception>
        <dependency>org.sonatype.plexus:plexus-cipher</dependency>
        <expected>1.7</expected>
        <resolved>1.4</resolved>
    </exception>
</exceptions>
```

In this case, the `1.4` version of the dependency would be acceptable even if the build tree would require the `1.7` version.

The `groupId` and `artifactId` components of the dependency name can use wildcards. An empty element (group or artifact) is treated as a wildcard.


### Resolvers

The standard strategy for determining which version of an artifact is used matches the strategy that maven itself employs. This should be sufficient for most uses.

It is possible to configure specific strategies for subsets of artifacts (with a `resolver` configuration or even change the default strategy (using the `defaultStrategy` configuration).

A resolver elements contains of a versioning strategy name and one or more include patterns to select the strategy for artifacts:

```xml
<configuration>
    <resolvers>
        <resolver>
            <id>apache-dependencies</id>
            <strategyName>apr</strategyName>
            <includes>
                <include>commons-configuration:commons-configuration</include>
                <include>org.apache.*:</include>
            </includes>
        </resolver>
    </resolvers>
</configuration>
```

The following strategies are included:

#### `default` - the default strategy

This strategy matches the actual maven version resolution.

It assumes that all smaller versions are compatible when replaced with larger numbers and compares version elements from left to right. E.g. 3.2.1 > 3.2 and 2.1.1 > 1.0.

#### `apr` - Apache APR versioning (aka semantic versioning)

Three digit versioning, assumes that for two versions to be compatible, the first digit must be identical, the middle digit indicates backwards compatibility (i.e. 1.2.x can replace 1.1.x but 1.4.x can not replace 1.5.x) and the third digit signifies the patch level (only bug fixes, full API compatibility).

#### `two-digits-backward-compatible` - Relaxed APR versioning

Similar to APR, but assumes that there is no "major" version digit (e.g. it is part of the artifact Id). All versions are backwards compatible. First digit must be the same or higher to be compatible (i.e. 2.0 can replace 1.2).

#### `single-digit` - Single version number

The version consists of a single number. Larger versions can replace smaller versions. The version number may contain additional letters or prefixes (i.e. r08 can replace r07).


## Legal

This is a friendly fork and rewrite of the [original dependency-version-check plugin](https://github.com/ning/maven-dependency-versions-check-plugin).

Licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

&copy; 2010 Ning, Inc.

&copy; 2011 Henning Schmiedehausen

&copy; 2020-2021 the basepom project
