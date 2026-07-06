# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/), and this
project adheres to [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

### Changed

### Deprecated

### Removed

### Fixed

## [0.8.0] - 2026-07-06

### Changed

- If an decoder (including builtin) cannot properly parse or throws an exception, then return the
  unparsed tagged value.

## [0.7.0] - 2026-06-26

### Added

- Quote non-tagged value that looks like a tagged value

## [0.6.0] - 2026-06-25

### Added

- Support `#integer` tag
- If a decoder throws, then just return the tagged value

## [0.5.0] - 2026-06-23

### Added

- Support maps with complex (non-string, non-symbol, non-keyword) keys
- Encode seqs as vectors
- Round-trip encoding for list and PersistentQueue

### Changed

- Allow overriding a built-in decoder
- Lots of performance improvements

## [0.4.0] - 2025-12-01

### Changed

- Make tag?, tagged-value?, enargus-key, deargus-key public since they are useful when working with
  data.
- Allow defining a new encoder to a built-in tag.  This allows normalizing types.  Redefining the
  decoder for a built-in tag is still disallowed.
- Allow recursive encoding.  An encoder can encode from non-base type to non-base type to reuse an
  existing encoding function.

## [0.3.2] - 2025-10-05

### Changed

- Documentation updates

## [0.3.1] - 2025-10-05

### Changed

- Test and documentation updates

## [0.3.0] - 2025-10-04

### Added

 - Encode keywords, symbols, strings as map keys
 - Validate encoder and decoder tags
 - Validate tags for function encoder
 - Test coverage
 - Detailed documentation

### Changed

- Corrected tag validation.  No longer looks for slash, but dot to separate segments
- Ensure Clojure and ClojureScript produce same Argus values

## [0.2.0] - 2025-07-17

### Added

- Support JavaScript Date and google.date.Date.

## [0.1.0] - 2025-07-15

### Added

- Initial release.

[unreleased]: https://github.com/thoughtfull-clojure/argus/compare/v0.8.0...main
[0.8.0]: https://github.com/thoughtfull-clojure/argus/releases/tag/v0.7.0..v0.8.0
[0.7.0]: https://github.com/thoughtfull-clojure/argus/releases/tag/v0.6.0..v0.7.0
[0.6.0]: https://github.com/thoughtfull-clojure/argus/releases/tag/v0.5.0..v0.6.0
[0.5.0]: https://github.com/thoughtfull-clojure/argus/releases/tag/v0.4.0..v0.5.0
[0.4.0]: https://github.com/thoughtfull-clojure/argus/releases/tag/v0.3.2..v0.4.0
[0.3.2]: https://github.com/thoughtfull-clojure/argus/releases/tag/v0.3.1..v0.3.2
[0.3.1]: https://github.com/thoughtfull-clojure/argus/releases/tag/v0.3.0..v0.3.1
[0.3.0]: https://github.com/thoughtfull-clojure/argus/releases/tag/v0.2.0..v0.3.0
[0.2.0]: https://github.com/thoughtfull-clojure/argus/releases/tag/v0.1.0..v0.2.0
[0.1.0]: https://github.com/thoughtfull-clojure/argus/releases/tag/v0.1.0
