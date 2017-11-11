# sbt-node
Plugins for integration of Node.js based build tools with sbt.

#### Contents:
* [Getting Started](#getting-started)
* [Plugins](#plugins)
  * [NpmPlugin](#npmplugin)
  * [SystemJSPlugin](#systemjsplugin)
  * [LiteServerPlugin](#liteserverplugin)
  * [SassPlugin](#sassplugin)
* [Helper Plugins](#helper-plugins)
  * [AssetsPlugin](#assetsplugin)
  * [ConfigPlugin](#configplugin)

## Getting Started

Add this to your `project/plugins.sbt`
```scala
addSbtPlugin("de.surfice" % "sbt-node" % "0.0.4")
```
and then enable the plugins you want to use (see below) for your project(s) in `build.sbt`:
```scala
lazy val root = project.in(file("."))
  .enablePlugins(NpmPlugin, /* ... */)
  /* ... */
```

## Plugins
The following sections list the sbt settings and tasks provided by each plugin.

**Note**: If a plugin task depends on configuration files generated from settings (e.g. `package.json`), then these files will be updated if required, and the tasks using those files (e.g. `npmInstall`) will be run automatically.

### NpmPlugin
This is the foundation required by all other plugins, but it can also be used on its own.
It provides integration with the `npm` tool:
- defining basic `npm` settings including dependencies and scripts
- generating the `package.json` for the project
- installing dependencies and running npm tasks

#### sbt Settings
- **`npmTargetDir`**: the root directory of the Node.js project; `node_modules` will be installed here unless `npmNodeModulesDir` is overriden.
- **`npmNodeNodeModulesDir`**: full path to the `node_modules` dir. This can be set separately from `npmTargetDir` in case multiple projects shall share the same `node_modules` directory.<br/>
  *Note*: `npmNodeModulesDir` must be located in `npmTargetDir` or in any parent directory thereof!
- **`npmDependencies`**: list of npm dependencies (name and version) the application/ library depends on at run time.<br/> 
  *Example*: `npmDependencies ++= Seq("rxjs" -> "^5.0.1")`
- **`npmDevDependencies`**: list of npm compile time / development dependencies.<br/>
  *Example*: `npmDevDependencies ++= "Seq("lite-server" -> "^2.2.2")`
- **`npmMain`**: value of the `main` property in the generated `package.json`.
- **`npmScripts`**: sequence of key/value pairs to be added to the `scripts` section in the generated `package.json`.
  *Example*:<br/>
  `npmScripts ++= Seq("start" -> "lite-server")`<br/>
  will add the following entry to the generated `package.json`:<br/>
  `"scripts": { "start": "lite-server" }`
  
#### sbt Tasks
- **`npmInstall`**: updates the `package.json` file and installs all dependencies. It is usually not necessary to call this task explicitly, since all tasks requiring npm packages should depend on this task.
- **`npmRunScript <NAME>`**: runs the npm script `NAME` (must be defined in `npmScripts`). If the script does not stop on its own, it can be killed by pressing RETURN in the sbt console.
- **`npmWritePackageJson`**: generates the project `package.json` file. It's usually not necessary to call this task explicitly, since it is called by `npmInstall`.

#### package.conf values
The follwing settings can be set by `package.conf` files located in any library JAR or in an (optional) `project.conf` (see [ConfigPlugin](#configplugin)):
- **`npm.dependencies`**: list of npm package name/version pairs to be added to the `npmDependencies`<br/>
  *Note*: always add your values to the existing list by referencing `{npm.dependencies}`, otherwise the values defined by any other `package.conf` will be overwritten (see example under [ConfigPlugin](#configplugin)).
- **`npm.devDependencies`**: list of npm package name/version pairs to be added to the `npmDevDependencies`<br/>
  *Note*: always add your values to the existing list by referencing `{npm.devDependencies}`, otherwise the values defined by any other `package.conf` will be overwritten (see example under [ConfigPlugin](#configplugin)).
 

### SystemJSPlugin
This plugin generates the `system.config.js` files with all necessary mappings for npm packages for use of the [System.js module loader](https://github.com/systemjs/systemjs).

#### sbt Settings
- **`systemJSFile`**: output path for the generated `system.config.js` (scoped to `fastOptJS` or `fullOptJS`).

#### sbt Tasks
- **`systemJSConfig`**: the configuration object used for generating the `system.config.js` file (scoped to `fastOptJS` or `fullOptJS`).
- **`systemJS`**: generates the `system.config.js` file (scoped to `fastOptJS` or `fullOptJS`). It's usually not necessary to call this task explicitly, since it is called by dependent tasks, e.g. `liteServerPrepare`.

#### package.conf values
- **`systemjs.map`**: System.js mappings key/value mappings to be added to the `system.config.js` file.
- **`systemjs.meta`**: additional entries for the `meta` section in the `system.config.js` file.
- **`systemjs.packages`**: additional entries for the `packages` section in the `system.config.js` file.
- **`systemjs.paths`**: additional path definitions to be added to the `system.js.config` file.

*Example*: see [ConfigPlugin](#configplugin).

### LiteServerPlugin
Configure and run `lite-server` from within sbt.
#### sbt Settings
- **`liteServerVersion`**: version of `lite-server` to be used.
- **`liteServerConfigFile`**: path to the `lite-server` config file (scoped to `fastOptJS` or `fullOptJS`.<br/>
  *Example*: `liteServerConfigFile in (Compile, fastOptJS) := baseDirectory.value / "my-config.json"`
- **`liteServerBaseDir`**: base directory from which files are served (scoped to `fastOptJS` or `fullOptJS`).
- **`liteServerIndexFile`**: path to the `index.html` file (scoped to `fastOptJS` or `fullOptJS`).
- **`liteServerRoutes`**: entries to be put in the lite-server config `routes` object (scoped to `fastOptJS` or `fullOptJS`).

#### sbt Tasks
- **`liteServerWriteConfigFile`**: writes the `lite-server` configuration file.
- **`liteServerWriteIndexFile`**: writes the `index.html` file for the specified stage (`fastOptJS` or `fullOptJS`).
- **`liteServerStart`**: starts the `lite-server` for the specified stage (`fastOptJS` or `fullOptJS`).<br/>
  *Example*: `> fastOptJS/liteServerStart`
- **`liteServerStop`**: stops the `lite-server` for the specified stage (`fastOptJS` or `fullOptJS`).

### SassPlugin
Compiles scss/sass files to css.

#### sbt Tasks
- **`sassTarget`**: returns the target directory for compiled sass files (overwrite this task to change the target dir).
- **`sassSourceDirectories`**: returns a list of source directories to be searched for sass files (overwrite this task to change/ add source directories).
- **`sassInputs`**: returns all input files that are compiled by the `sass` task (if necessary).
- **`sass`**: compiles scss files located in `sassSourceDirectories`. It's usually not necessary to call this task explicitly, since `fastOptJS` and `fullOptJS` depend on this task if the `SassPlugin` is loaded.

## Helper Plugins
### AssetsPlugin
TBD

### ConfigPlugin
This plugin loads additional configuration values found in any `package.conf` contained within a library JAR, or in a `project.conf` file located in the project root. The format of these files is [HOCON](https://github.com/typesafehub/config).

This plugin is automatically activated by NpmPlugin or any plugin depending on that.

#### sbt Settings
- **`npmProjectConfigFile`**: file from which additional project-specific config values are loaded (default: `project.conf`)

#### sbt Tasks
- **`npmProjectConfig`**: loads all `package.conf` files found in libraries (and the `npmProjectConfigFile`) and returns the parsed Typesafe [Config](https://github.com/typesafehub/config) object.
- **`npmProjectConfigString`**: concatenates all detected configuration files into a single string; use `show npmProjectConfigString` to view the configuration evaluated by `npmProjectConfig`.

#### Example package.conf
```
npm {
  // add the npm packages `foo` and `bar` to the list of npmDependencies
  dependencies = ${npm.dependencies} [
    {"foo" = "^0.2.0"}
    {"bar" = "^0.3.2"}
  ]
  
  // add npm package `node-sass` to the list of npmDevDependencies
 devDependencies = ${npm.devDependencies} [
    {"node-sass" = "^4.5.3"}
  ]
}

// System.js configuration
systemjs {
  // add a mapping for package `foo` to the system.config.js file
  // note: the prefix 'npm:' is defined by the SystemJSPlugin and points to the project node_modules dir
  map {
    "foo" = "npm:/foo/bundles/foo.umd.js"
  }
  
  meta {
    // use `text_loader` for HTML files
    "*.html" {
      loader = "text_loader"
    }
  }
}
```
