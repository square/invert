# Change Log

_2024-11-27_

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
