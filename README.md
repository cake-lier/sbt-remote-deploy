# sbt-remote-deploy

[![Build status](https://github.com/cake-lier/sbt-remote-deploy/actions/workflows/publish.yml/badge.svg)](https://github.com/cake-lier/sbt-remmote-deploy/actions/workflows/publish.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.cake-lier/sbt-remote-deploy/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.cake-lier/sbt-remote-deploy/)
[![semantic-release: conventional-commits](https://img.shields.io/badge/semantic--release-conventional_commits-e10098?logo=semantic-release)](https://github.com/semantic-release/semantic-release)
[![License: MIT](https://img.shields.io/github/license/cake-lier/sbt-remote-deploy)](https://github.com/cake-lier/sbt-remote-deploy/blob/main/LICENSE)
[![Scaladoc](https://img.shields.io/badge/scaladoc-latest-brightgreen)](https://cake-lier.github.io/sbt-remote-deploy/io/github/cakelier/)

A sbt plugin for deploying one or more scala artifacts remotely.

## How to install

Simply add the following line to your ``project/plugins.sbt``

```scala
addSbtPlugin("io.github.cake-lier" % "sbt-remote-deploy" % "2.0.0")
```

and then enable the plugin in your ``build.sbt`` file like so

```scala
lazy val root = project
  .in(file("."))
  .enablePlugins(RemoteDeployPlugin)
```

## How to use

### Documentation

For first, you need to configure the remote location or the remote locations you want to access with this plugin. You must 
specify the host to reach and the user to use, as you would do if you were starting an SSH connection. You can also specify 
the port of the machine to which connect, but if not specified, the default is the port 22. You can also specify the method to 
be used for connecting to the remote location. If a password-based authentication is used, you can supply a password or, if a 
private key-based authentication is used, you can specify the path to the local file containing the key and the possible 
passphrase used for encrypting the key. Other configuration parameters include the fingerprint for the remote location 
identity, which can be supplied for checking if the two match, and a parameter to be thorough with the identity verification 
process, which is by default `true`. No worries if you already connected with the remote, and you saved its identity to the 
`known_hosts` file, the default files containing already known hosts are loaded by default, if found on your local machine. 
For knowing how to specify those parameters within a configuration file, jump to section 
"[Configuration file](#configuration-file)". If you want to specify your configuration via code, for example in your `build.
sbt` file, jump to section "[DSL](#dsl)".

After creating your configurations, you just need to put them where the plugin can find them. The setting key 
`remoteDeployConfFiles` can be used for specifying the path **relative** to the current project folder of all your configuration 
files. This folder is then considered the base folder for each configuration file path. If you specified your configurations 
through code, you can use it into the setting key `remoteDeployConfs`, which accepts a pair made of the name of the 
configuration and the configuration itself. When you have done this, your configurations are ready to be used!

The last step is to specify the artifacts to deploy and the operations to be performed before and after the deployment happens.
Through the setting key `remoteDeployBeforeHook` you can specify a function which receives an object as input that allows you 
to run commands on the remote location, as they were launched in a terminal, before the deployment of the artifact is done. 
You can specify all preparation operations such as cleaning, directory creation, etc. The setting key `remoteDeployAfterHook` 
is similar, the only difference is that the function will be called only after the copy of all the artifacts has completed 
with success, otherwise this callback won't be called. All commands specified in those functions are executed asynchronously, 
because they can take a long time for completing, but no worries! All the output will be copied onto your standard output and 
standard error, so you can see what is happening on your remote machine... Unless you don't want to, but the plugin has you 
covered also in this case.

Last, but not least, the `remoteDeployArtifacts` setting key, which is to be used for specifying the artifacts to deploy 
remotely. This key accepts pairs of two things: the local `sbt.File` to be deployed to the remote location and the 
**absolute** remote path to which deploy the file, which must contain also the name of the file itself, not necessarily the 
same of the one in the local machine. Being the local file an `sbt.File`, this plugin is thought for working hand in hand 
with the "sbt-assembly" plugin and the default `compile` task.

When everything is configured correctly, you can launch your remote deploy task. Just write on your terminal `sbt remoteDeploy 
<name of the configuration>` and the plugin will load all your configurations, search for the one with the name you inserted 
and use it in its deployment pipeline.

### Setting keys

| Setting name             | Description                                                                                                    | Default value |
|--------------------------|----------------------------------------------------------------------------------------------------------------|---------------|
| `remoteDeployConfFiles`  | a `Seq` of strings, each of which is the path to a file containing the remotes configurations.                 | `Seq.empty`   |
| `remoteDeployConfs`      | a `Seq` of pairs made from the name of the configuration and the object representing the configuration itself. | `Seq.empty`   |
| `remoteDeployArtifacts`  | a `Seq` of pairs made from the local file to be copied and the remote path to which it should be copied.       | `Seq.empty`   |
| `remoteDeployBeforeHook` | an `Option` containing a function which can be used to perform commands on the remote before the copy happens. | `None`        |
| `remoteDeployAfterHook`  | an `Option` containing a function which can be used to perform commands on the remote after the copy happened. | `None`        |

### Configuration file

A sample file configuration is the one that follows.

```HOCON
remotes {
  test {
    host = localhost
    user = me
    password = example
    verifyIdentity = false
  },
  production {
    host = example.org
    user = root
    port = 2022
    privateKeyFile = /home/me/.ssh/non_standard_key_name
    privateKeyPassphrase = example
    fingerprint = d3:5e:40:72:db:08:f1:6d:0c:d7:6d:35:0d:ba:7c:32
  }
}
```

The file format is the "[HOCON](https://github.com/lightbend/config)" one, developed by Lightbend as a JSON superset. A root 
object must be specified with the key "remotes", which should contain an object for each configuration. The keys that can be 
specified are the ones present in the example, so `host`, `port`, `user`, `password`, `privateKeyFile`, `privateKeyPassphrase`,
`fingerprint`, `verifyIdentity`. To know what they mean and if they are mandatory or not, please refer to the 
"[Documentation](#documentation)" section. Every field needs a string value except for the `port` one, which accepts only 
integers, and the `verifyFingerprint`, which accepts only boolean values. The configuration file format tries to be as close as 
possible to the [DSL specification](#dsl).

### DSL

The configurations, along with their names, can be specified in the `build.sbt` file using the DSL as follows.

```scala
remoteDeployConf := Seq(
  remoteConfiguration(withName = "test") {
    has host "localhost"
    has user "me"
    has password "example"
    has verifyIdentity false
  },
  remoteConfiguration(withName = "production") {
    has host "example.org"
    has user "root"
    has port 2022
    has privateKeyFile "/home/me/.ssh/non_standard_key_name"
    has privateKeyPassphrase "example"
    has fingerprint "d3:5e:40:72:db:08:f1:6d:0c:d7:6d:35:0d:ba:7c:32"
  }
)
```

The fields that can be specified for tuning the parameters of a remote configuration are the ones present in the example, so 
`host`, `port`, `user`, `password`, `privateKeyFile`, `privateKeyPassphrase`, `fingerprint`, `verifyIdentity`. To know what 
they mean and if they are mandatory or not, please refer to the "[Documentation](#documentation)" section. Every field needs a 
string value except for the `port` one, which accepts only integers, and the `verifyFingerprint`, which accepts only boolean 
values. The DSL tries to be as close as possible to the [configuration file format](#configuration-file).

### Hooks

An example of a possible hook is the one that follows.

```scala
remoteDeployBeforeHook := Some(remote =>
  (for {
    result <- remote.run("mkdir test")
  } yield result.exitCode)
    .onComplete {
      case Success(Some(c)) => println(c)
      case _ => println("Error")
    }
)
```

The function receives as a parameter a "remote" objects which provides the methods to be called for executing commands on the 
remote machine. Its results can then be inspected when the command completed its execution. While executing, all output from 
each command will be printed to the command line.

## ⚠️⚠️⚠️ Warnings ⚠️⚠️⚠️

Every configuration, being in a configuration file or directly specified in the ``build.sbt`` file, must have a unique name. 
If the name is not unique, clashes may happen between configurations and only the last one specified is kept. 
In this operation, configurations specified in the ``build.sbt`` file have more precedence than the ones in the configuration files.
