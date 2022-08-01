# [2.0.0](https://github.com/cake-lier/sbt-remote-deploy/compare/v1.0.2...v2.0.0) (2022-08-01)


### Bug Fixes

* fix capitalization error, run scalafix ([6d9033c](https://github.com/cake-lier/sbt-remote-deploy/commit/6d9033c6f0eb7de74c51cd3275e6944788898e0c))
* fix disabling of identity verification in tests ([9254f62](https://github.com/cake-lier/sbt-remote-deploy/commit/9254f629b3ce1494c77ff7a5a5b50914f09ec570))
* fix expected output of before deploy command in tests, change command to be 'mkdir' to be more integrated with test ([367b7f5](https://github.com/cake-lier/sbt-remote-deploy/commit/367b7f56f43261966398e1084c8d52ad69133267))
* handle exception during known hosts loading when files are absent ([5cd8407](https://github.com/cake-lier/sbt-remote-deploy/commit/5cd840709a883e15a6e837dea6db31c4a95fe7ce))
* improve tests changing key names and adding test for before hook ([ccbee7a](https://github.com/cake-lier/sbt-remote-deploy/commit/ccbee7a87d234fe02f7ebf2e122f39f919602625))


### Features

* add support for before-deployment hooks ([be89721](https://github.com/cake-lier/sbt-remote-deploy/commit/be89721d705f809df7c05aea79e751a4cc08f1cc))
* add support for fingerprint use ([caa78d4](https://github.com/cake-lier/sbt-remote-deploy/commit/caa78d4022f6e88418df14107de138ca0b6a149e))
* add support for skipping or forcing verification process ([b282614](https://github.com/cake-lier/sbt-remote-deploy/commit/b28261471cd1b9830dc63908feb29602fe5e3d74))
* add support for user visualization of validation errors in file parsing and field values ([0044d38](https://github.com/cake-lier/sbt-remote-deploy/commit/0044d38191ecec0967e22a4981d5b274f0b0909c))

## [1.0.2](https://github.com/cake-lier/sbt-remote-deploy/compare/v1.0.1...v1.0.2) (2022-07-24)


### Bug Fixes

* update code for wartremover and scalafix ([b829e07](https://github.com/cake-lier/sbt-remote-deploy/commit/b829e0765194751a4ff0420de92d3bcf373836d8))
