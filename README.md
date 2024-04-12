# 🔃 Invert
Invert is a Gradle Plugin with a dynamic web report that lets you gain insights into your Gradle project via static analysis.

**NOTE: APIs and internals may change as it's used by Square's 6,000 module Gradle project internally and requirements are heavily driven by Square's needs.**

Invert is a Gradle Plugin with a dynamic web report that lets you gain insights into your Gradle project via static analysis.  Instead of seeing what a module depends on, you can see the inverted view of who depends on it.  This is done by fully configuring your project and using transitive dependencies by configuration to calculate it.  Additionally it is a great tool for collecting static analysis data about your project, by module.  Custom static analysis plugins can be added, based on your project’s needs.  The report can be computed on every merge to your main branch (via GitHub actions + GitHub pages), to have an always up-to-date view of your project without waiting for your project to configure.

# Installing Invert

## Initialization Script (Great for trying out!)
Copy and paste `invert.gradle` into your project's root folder, then run the following:
`./gradlew --init-script invert.gradle :invert`

This will run the `Invert` report on your project with no other work!

## Standard Plugin Installation


# Running Invert
`:invert`