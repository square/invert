# Change Log

_2024-11-27_

## Version 0.0.5-dev

* Upgraded to Kotlin 2.1.21 and updated other libraries to compatible versions.
* Replaced deprecated `getDependencyProject()` method with Gradle 9-compatible version.
* Renamed "Tech Debt" page to "Stats Burndown" for better clarity.
* Fixed Kotlin 2.1.21 webpack resources issue.
* Updated documentation with proper snapshot version information.

**Full Changelog**: https://github.com/square/invert/compare/0.0.4-dev...0.0.5-dev

## Version 0.0.4-dev
* Added uniqueIdentifier to `CodeReference` to allow for easier linking.
* Added Sarif support to export `CodeReference`s in the Sarif format.
* Simplified the "Stat Detail" page to only include a single statKey as a param instead of a list.  We weren't really using that feature anyways.
* Added collectors to init scripts. Also publishing all artifacts.
* Added new 'configureInvert' callback before invert is loaded.
* Added line chart support.
* Updated CodeReferences and Owner Breakdown pages.
* Compute totals by module and by ownership.
* Added sorting for numeric extras!
* Added a new AllOwners field to the InvertOwnershipCollector
* Added Tech Debt Page w/embed view
**Full Changelog**: https://github.com/square/invert/compare/0.0.3-dev...0.0.4-dev

## Version 0.0.3-dev
* Added in the ability to attach "extras" key/value pairs to a `CodeReference`
* Renamed `gradlePath` -> `modulePath` to be more general.
* Removed the "Dependency Injection" page that was not generally applicable.
* Cleaned up "Custom Pages" and now showing them first.
* Filtering of `CodeReference`s by owner and module.
* Added a `CodeReference`s report page.
* Added first version of the "Owner Breakdown" page to show `CodeReference`s tallied by owner.

**Full Changelog**: https://github.com/square/invert/compare/0.0.2-dev...0.0.3-dev

## Version 0.0.2-dev

_2024-09-03_

* Introduced the "aggregate" phase which allows analysis to be done once all projects have been analyzed.  This gives enables more insights regarding cross-project information and collected stats.
* Added a "category" to `StatMetadata` which creates groupings in the left nav.
* Added a new tabular view for `CodeReference`s.
* Shows stat totals on the home page.
* Bug fixes and other small changes.  [See all changes here](https://github.com/square/invert/compare/0.0.1-dev...0.0.2-dev).

## Version 0.0.1-dev

_2024-06-30_

* Initial Development Versions.  See https://github.com/square/invert/releases for more details.
