# sbt-alldocs

Put **all** the documentation for a project in one place. Defaults to an `alldocs` folder under project root.
Suitable for distribution of an alldocs for an alljar (aka uberjar). This will include all dependencies and all compiler
version targets. The result is a large but complete set of documentation.

EG: screenshot of the generated `alldocs/index.html` for [docs.glngn.com](http://docs.glngn.com/latest/api/).

![index screenshot](https://github.com/glngn/sbt-alldocs/raw/master/screenshot.png "Screenshot of an example index")

## Usage

Add to `project/plugins.sbt`:

~~~
addSbtPlugin("com.glngn" % "sbt-alldocs" % "0.2.1")
~~~

At `sbt` prompt:

~~~
sbt> update ; allDocs
~~~

Open in web browser: `alldocs/index.html`.

This plugin requires sbt 1.2.0+

This adds a command `allDocs` which:

1. Collects all documentation artifacts for dependencies. This is all dependencies minus those in `allDocsExclusions`.
2. Expands all documentation artifacts into `allDocsTargetDir`. Which defaults to `alldocs`.
3. Generates all documentation for all projects. These are copied under `allDocsTargetDir`.
4. Finally, generates an `index.html` in `allDocsTargetDir` that is an index of all the collected documentation.

### Options

Note: the options refer to `name` and this is ill-defined. This is exactly the link text. Easiest to
generate all docs then look at link text to determine `name`.

* `allDocsExclusions` - Set of strings of the `name` for the document artifact to exclude.
* `allDocsRenames` - Map of names to text. This is applied after exclusions.
* `allDocsTargetDir` - Directory relative to root to output. Defaults to `alldocs`.
* `allDocsSections` - Map of regex to (priority, section text).

EG:

~~~
allDocsSections := Seq(
    "akka-.*" -> (20, "Akka"),
    "log4j-.*" -> (30, "Logging (Log4j 2)"),
    "slf4j-.*" -> (30, "Logging (Log4j 2)"),
    "scala-.*" -> (10, "Scala Standard Libraries"),
    "scalaz-.*" -> (15, "Scalaz Libraries"),
    ".*" -> (999, "Other Included Libraries")
)
~~~

## Testing

Run `test` for regular unit tests.

Run `scripted` for [sbt script tests](http://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html).

## TODO

1. [Claim your project an Scaladex](https://github.com/scalacenter/scaladex-contrib#claim-your-project)

## Notes

This command may take a while. All dependencies for all compiler targets is, even on small projects,
a large number of files to decompress from jar files. In particular, I've found windows to be
exceptionally slow at this. The recommended pattern is for the organizations CI/CD to deploy this
artifact to a local server.

Failure to generate project documentation does not result in the command failing. The project
documentation will be, however, missing from the index. The first part is intentional. The second
part contains a bug. The goal is to support fetching the docs of new dependencies before the project
even compiles. The issue is any exist project docs should still be included in the index.
