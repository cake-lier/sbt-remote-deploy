# sbt-remote-deploy

[![Build status](https://github.com/cake-lier/sbt-remote-deploy/actions/workflows/publish.yml/badge.svg)](https://github.com/cake-lier/sbt-remmote-deploy/actions/workflows/publish.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.cake-lier/sbt-remote-deploy/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.cake-lier/sbt-remote-deploy/)
[![semantic-release: conventional-commits](https://img.shields.io/badge/semantic--release-conventional_commits-e10098?logo=semantic-release)](https://github.com/semantic-release/semantic-release)
[![License: MIT](https://img.shields.io/github/license/cake-lier/sbt-remote-deploy)](https://github.com/cake-lier/sbt-remote-deploy/blob/main/LICENSE)
[![Scaladoc](https://img.shields.io/badge/scaladoc-latest-brightgreen)](https://cake-lier.github.io/sbt-remote-deploy/io/github/cakelier/)

A sbt plugin for deploying one or more scala artifacts remotely.

## How to install

Simply add the following line to your ``project/plugins.sbt``:

``` scala
addSbtPlugin("io.github.cake-lier" % "sbt-remote-deploy" % "2.0.0")
```

## How to use

### Settings and tasks

This plugin provides one task to be run from the command line and four settings to be configured in the ``build.sbt`` file on a per-project basis.
The four settings are:

* **remoteDeployConfFiles**: a Seq of strings, each of which is the **relative** path of a file containing remotes configurations. It should be used when your configurations are specified in a file, which should be put inside the project folder. This folder is considered the base folder for the path of the configuration file.
* **remoteDeployConfs**: a Seq of pairs made from the name of the configuration and the object representing the configuration itself. It should be used when specifying the configuration directly from the ``build.sbt`` file.
* **remoteDeployArtifacts**: a Seq of pairs made from the local to be copied to the remote location and the **absolute** remote path in which it should be copied. The remote path should also contain the name of the file after being copied, which could be the same of the one in the local machine. The local file must be specified as a ``sbt.File``. This is because it is more easy to specify the products of the other tasks as files to be transferred.
* **remoteDeployAfterHooks**: a Seq of functions which every one of them give you access to a "SshClient" instance which can be used to perform commands on the remote after the file copy has been completed.

The task which the plugin provides is called "**remoteDeploy**" and it must be given the name of the remote configuration you intend to use for deploying on a remote machine the specified artifact(s) and on which execute the specified hook(s).

### Configuration file format

A sample file configuration is the one that follows:

``` HOCON
remotes {
  test {
    host = localhost
    user = me
    password = example
  },
  production {
    host = example.org
    user = root
    port = 2022
    privateKeyFile = /home/me/.ssh/id_rsa
    privateKeyPassphrase = example
  }
}
```

### DSL for specifying the configurations in the ``build.sbt`` file

The configurations, along with their name, can be specified in the ``build.sbt`` file using a DSL as follows:

``` scala
remoteConfiguration(withName = "test") {
  has host "localhost"
  has user "me"
  has password "example"
},
remoteConfiguration(withName = "production") {
  has host "example.org"
  has user "root"
  has port 2022
  has privateKeyFile "/home/me/.ssh/id_rsa"
  has privateKeyPassphrase "example"
}
```

## ⚠️⚠️⚠️ Warnings ⚠️⚠️⚠️

Every configuration, being in a configuration file or directly specified in the ``build.sbt`` file, must have an unique name. 
If the name is not unique, clashes may happen between configurations and only the last one specified is kept. 
In this operation, configurations specified in the ``build.sbt`` file have more precedence than the ones in the configuration files.

The host and the user parameters of a configuration are the only ones which are required. 
If they are missing, the "remoteDeploy" task will fail with an exception.
