# 🔃 Invert
[![Maven Central](https://img.shields.io/badge/dynamic/xml?url=https://repo1.maven.org/maven2/com/squareup/invert/invert-gradle-plugin/maven-metadata.xml&label=Latest%20Stable&color=blue&query=.//versioning/latest)](https://repo1.maven.org/maven2/com/squareup/invert/com.squareup.invert.gradle.plugin/)
[![Latest Snapshot](https://img.shields.io/badge/dynamic/xml?url=https://s01.oss.sonatype.org/content/repositories/snapshots/com/squareup/invert/com.squareup.invert.gradle.plugin/maven-metadata.xml&label=Latest%20Snapshot&color=orange&query=.//versioning/latest)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/squareup/invert/com.squareup.invert.gradle.plugin/)
[![LICENSE](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/square/invert/blob/main/LICENSE)

Invert is a Gradle Plugin with a dynamic web report that lets you gain insights into your Gradle project via static analysis.

**NOTE: APIs and internals may change as it's used by Square's 6,000+ module Gradle project internally and requirements are heavily driven by Square's needs.**

Invert is a Gradle Plugin with a dynamic web report that lets you gain insights into your Gradle project via static analysis.  Instead of seeing what a module depends on, you can see the inverted view of who depends on it.  This is done by fully configuring your project and using transitive dependencies by configuration to calculate it.  Additionally it is a great tool for collecting static analysis data about your project, by module.  Custom static analysis plugins can be added, based on your project’s needs.  The report can be computed on every merge to your main branch (via GitHub actions + GitHub pages), to have an always up-to-date view of your project without waiting for your project to configure.

# Installing Invert

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
