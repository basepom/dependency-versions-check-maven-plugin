# Changes

This is the changelog for the maven dependency versions check plugin. It follows [Keep a Changelog v1.0.0](http://keepachangelog.com/en/1.0.0/).

## 3.2.0 - 2021-04-01

### Fixed

* unresolvable optional dependencies were not properly ignored and lead to the plugin failing. By default, the plugin now skips unresolvable optional dependencies. The old behavior can be restored with the `dvc.optional-dependencies-must-exist` command line switch or the <optionalDependenciesMustExist>` config setting. Default is `false` (optional dependencies are ignored.

* in fast resolution mode, when a dependency resolution failed, the plugin tried to cancel the other operations in flight which led to a lot of stack traces printed on the console. This has been fixed.

### Changed

* The error message when a dependency could not be resolved has been clarified and is now the same in fast and slow resolution mode.

* update site generation


## 3.1.0 - 2021-22-03

* First usable release

## 3.0.0 - 2021-19-03

* Pre-release.
