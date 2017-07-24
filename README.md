# sbt-node
Plugins for integration of Node.js based build tools with sbt.

#### Contents:
* [Getting Started](#getting-started)
* [Plugins](#plugins)
  * [NpmPlugin](#npmplugin)
  * [SystemJSPlugin](#systemjsplugin)
  * [LiteServerPlugin](#liteserverplugin)

## Getting Started

Add this to your `project/plugins.sbt`
```scala
addSbtPlugin("de.surfice" % "sbt-node" % "0.0.1")
```
and then enable the plugins you want to use (see below) for your project(s) in `build.sbt`:
```scala
lazy val root = project.in(file("."))
  .enablePlugins(NpmPlugin, /* ... */)
  /* ... */
```

## Plugins
The following sections list the sbt settings and tasks provided by each plugin.

**Note**: If a plugin task depends on configuration files generated from settings (e.g. `package.json`), then these files will be updated if required, and the tasks using those files (e.g. `npmInstall`) will be run will be automatically.

### NpmPlugin
This is the foundation required by all other plugins, but it can also be used on its own.
It provides integration with the `npm` tool:
- defining basic `npm` settings including dependencies and scripts
- generating the `package.json` for the project
- installing dependencies and running npm tasks

#### Settings
- **`npmTargetDir`**: the root directory of the Node.js project; `node_modules` will be installed here unless `npmNodeModulesDir` is overriden.
- **`npmNodeNodeModulesDir`**: full path to the `node_modules` dir. This can be set separately from `npmTargetDir` in case multiple projects shall share the same `node_modules` directory.<br/>
  *Note*: `npmNodeModulesDir` must be located in `npmTargetDir` or in any parent directory thereof!
- **`npmDependencies`**: list of npm dependencies (name and version) the application/ library depends on at run time.<br/> 
  *Example*: `npmDependencies ++= Seq("rxjs" -> "^5.0.1")`
- **`npmDevDependencies`**: list of npm compile time / development dependencies.<br/>
  *Example*: `npmDevDependencies ++= "Seq("lite-server" -> "^2.2.2")`
- **`npmScripts`**: map of `script` entries to be added to the `package.json` file.<br/>
  *Example*: `npmScripts ++= Seq( "start" -> "lite-server" )`
  
#### Tasks
- **`npmInstall`**: updates the `package.json` file and installs all dependencies.
- **`npmRunScript <NAME>`**: runs the npm script `NAME` (must be defined in `npmScripts`). If the script does not stop on its own, it can be killed by pressing RETURN in the sbt console.

### SystemJSPlugin
TBD

### LiteServerPlugin
Configure and run `lite-server` from within sbt.
#### Settings
- **`liteServerVersion`**: version of `lite-server` to be used.
- **`liteServerConfigFile`**: path to the `lite-server` config file (scoped to `fastOptJS` or `fullOptJS`.<br/>
  *Example*: `liteServerConfigFile in (Compile, fastOptJS) := baseDirectory.value / "my-config.json"`
- **`liteServerBaseDir`**: base directory from which files are served (scoped to `fastOptJS` or `fullOptJS`).
- **`liteServerIndexFile`**: path to the `index.html` file (scoped to `fastOptJS` or `fullOptJS`).
- **`liteServerRoutes`**: entries to be put in the lite-server config `routes` object (scoped to `fastOptJS` or `fullOptJS`).

#### Tasks
- **`liteServerWriteConfigFile`**: writes the `lite-server` configuration file.
- **`liteServerWriteIndexFile`**: writes the `index.html` file for the specified stage (`fastOptJS` or `fullOptJS`).
- **`liteServerStart`**: starts the `lite-server` for the specified stage (`fastOptJS` or `fullOptJS`).<br/>
  *Example*: `> fastOptJS/liteServerStart`
- **`liteServerStop`**: stops the `lite-server` for the specified stage (`fastOptJS` or `fullOptJS`).
