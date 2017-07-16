# sbt-node
Plugins for integration of Node.js based build tools with sbt.

#### Contents:
* [Getting Started](#getting-started)
* [Plugins](#plugins)
  * [NpmPlugin](#npmplugin)
  * [SystemJSPlugin](#systemjsplugin)

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
### NpmPlugin
This is the foundation required by all other plugins, but it can also be used on its own.
It provides integration with the `npm` tool:
- defining basic `npm` settings including dependencies and scripts
- generating the `package.json` for the project
- installing dependencies and running npm tasks

#### Settings
- **`npmTargetDir`**: the root directory of the Node.js project; `node_modules` will usually be installed here
- **`npmDependencies`**: list of npm dependencies (name and version) the application/ library depends on at run time.\\ 
  *Example*: `npmDependencies ++= Seq("rxjs" -> "^5.0.1")`
- **`npmDevDependencies`**: list of npm compile time / development dependencies.\\ 
  *Example*: `npmDevDependencies ++= "Seq("lite-server" -> "^2.2.2")`
- **`npmScripts`**: map of `script` entries to be added to the `package.json` file.\\ 
  *Example*: `npmScripts ++= Seq( "start" -> "lite-server" )`
  
#### Tasks
- **`npmInstall`**: updates the `package.json` file and installs all dependencies.
- **`npmRunScript <NAME>`**: runs the npm script `NAME` (must be defined in `npmScripts`). If the script does not stop on its own, it can be killed by pressing RETURN in the sbt console.

### SystemJSPlugin
TBD
