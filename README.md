# 🔃 Invert
[![Maven Central](https://img.shields.io/badge/dynamic/xml?url=https://repo1.maven.org/maven2/com/squareup/invert/invert-gradle-plugin/maven-metadata.xml&label=Maven%20Central&color=blue&query=.//versioning/latest)](https://repo1.maven.org/maven2/com/squareup/invert/com.squareup.invert.gradle.plugin/)
[![Latest Snapshot](https://img.shields.io/badge/dynamic/xml?url=https://s01.oss.sonatype.org/content/repositories/snapshots/com/squareup/invert/com.squareup.invert.gradle.plugin/maven-metadata.xml&label=Latest%20Snapshot&color=orange&query=.//versioning/latest)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/squareup/invert/com.squareup.invert.gradle.plugin/)
[![LICENSE](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/square/invert/blob/main/LICENSE)

# What is Invert?

Invert is a tool that collects and analyzes project statistics at scale, providing actionable insights through a dynamic web-based report. It helps developers and organizations track dependencies, migration progress, and code usage, making large-scale source management more efficient.

The name **Invert** originates from its initial purpose: a dynamic web report to explore the **inverted dependency graph** of a large multi-module project. While it has since evolved into a broader tool, this core capability remains. Invert continues to provide a unique perspective on a codebase by leveraging Collectors to extract insights, enabling developers and teams to see their project structure in a new light.

## Target Consumers
This tool is currently used on Android and iOS repositories at Square, and we are working to make it more generally useful.  There is quick plug-and-play capabilities with Gradle projects, but it can be used with any project that can be configured to use the Invert API.  It is especially useful for large projects with many modules, where it can be difficult to understand the full scope of the project.

At this point in time we have a plug-n-play method to use Invert from a Gradle project.  We have other mechanisms for integrating with other build systems, and will open source any if they end up being generally applicable.

### Note
**APIs and internals may change as it's used by Square's 6,600+ module Gradle project internally and requirements are heavily driven by Square's needs.**


### Why Use Invert?

Invert excels in large, source-based **monorepo** projects, enabling:

- Efficient **collection of stats** at scale.
- **Actionable insights** via dynamic reporting.
- Enhanced tracking for **dependencies**, **migrations**, and **ownership-based analysis**.

By leveraging **code references**, Invert transforms raw data into meaningful insights, making it easier for developers and leadership to drive informed decisions.

## Invert Internals 

Invert consists of two primary components:

1. **Collection** - Gathers statistics and data about a project, structured by module.
2. **Report** - A dynamic web UI that enables users to explore stats, track migrations, analyze dependencies, and gain additional insights.

### Invert Primitives

Invert operates on the following core concepts:

#### Modules

Each module is defined by its name and filesystem path. Additionally, a module may include:

- **Dependencies** *(Highly recommended)* - Capturing dependencies allows for deeper insights into module relationships.
- **Plugins** - Defines the type of module and its characteristics.

#### Ownership *(Optional)*

Modules and code references can have an assigned owner, which enhances the **Invert Report** by attributing insights to teams or individuals. If a code reference does not specify ownership, it inherits ownership from the module it is in. While optional, ownership is recommended when available.

#### Stats

Invert collects various types of statistics:

- **Code References** *(Most Valuable Feature)* - Represents occurrences of specific code references in a project.
- **Boolean** - Flags indicating the presence or absence of a feature.
- **Numerical** - Metrics such as counts or percentages.
- **String** - Text-based attributes associated with a module.

#### Code References

Code references provide precise data points:

- Each reference is stored with its **file path and line number**.
- Can optionally include a **code snippet**, which enables developers to take direct action.
- Facilitates both **quantitative analysis** and **actionable reporting**, helping developers and organizational leaders track progress effectively.

## Goals

### Overall
* Provide a tool to get an actionable, always up-to-date snapshot of what matters in your codebase.
* Tap into code ownership and provide information by owner/org.
* Surface static insights about your codebase via static analysis, by module.

### Web Report
* Provide dynamic report allowing exploration of data.
* Support dynamic loading of data
* Support Deep Links for All Report Pages
* Support Markdown Format
* Integrate/play nicely with GitHub to link to source files on GitHub.
* Provide a way for someone to extend the report to build their own.

### Architecture Diagram

```mermaid
graph LR;
    A[Collectors]
    A --> B[Shared Models]
    B --> C[Invert Web Report]
```

# Invert Gradle Plugin

Invert is a Gradle Plugin with a dynamic web report that lets you gain insights into your Gradle project via static analysis.  Instead of seeing what a module depends on, you can see the inverted view of who depends on it.  This is done by fully configuring your project and using transitive dependencies by configuration to calculate it.  Additionally it is a great tool for collecting static analysis data about your project, by module.  Custom static analysis plugins can be added, based on your project’s needs.  The report can be computed on every merge to your main branch (via GitHub actions + GitHub pages), to have an always up-to-date view of your project without waiting for your project to configure.

## Standard Plugin Installation

```
// Root build.gradle
plugins {
    id("com.squareup.invert") version "<<latest_version>>"
}
repositories {
    mavenCentral() // Released Versions
    maven { url "https://s01.oss.sonatype.org/content/repositories/snapshots" } // SNAPSHOT Versions
}
```

# Running Invert

```
./gradlew :invert
```

# Plugin Configuration (Optional)
``` kotlin
invert {
    addStatCollector(<<stat collector instance>>)
    includeSubproject {suproject: Project ->
        // By default this returns true, but you can use this filter to limit the projects that the root `:invert` task targets.
        true
    }
    includeConfigurations { project: Project, configurationNames ->
        // You can filter out the configurations you want to analyze.  By default all ending in `RuntimeClasspath` will be used.
        configurationNames
    }
    ownershipCollector(<<ownership collector instance>>)
}
```

# Generated Reports
The report will be generated at `build/reports/invert`.

## License
```
   Copyright (c) 2024 Block, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```
