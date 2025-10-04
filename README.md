# argus

<img align="left" src="./img/argus-logo-150.png" />

[![Lint and test](https://github.com/thoughtfull-clojure/argus/actions/workflows/lint-and-test.yml/badge.svg)](https://github.com/thoughtfull-clojure/argus/actions/workflows/lint-and-test.yml) [![clojars badge](https://badgen.net/badge/clojars/0.2.0/blue)](https://clojars.org/systems.thoughtfull/argus/versions/0.2.0) [![cljdoc badge](https://badgen.net/badge/cljdoc/0.2.0/blue)](https://cljdoc.org/d/systems.thoughtfull/argus/0.2.0/doc/readme)

Extended and extensible types for JSON.  An implementation of [Argus](https://argus.fyi/).

JSON is a popular, but anemic data format.  It supports four scalar types and two container types.
This is a far cry from the richness of the real world of programming languages, and yet ... it is a
robust and universal foundation if only it supported _tagged values_.

## Baseline versions

- JVM: 21
- Node: 20
- Clojure: 1.12.3

## Yet Another Data Format

I get it.  I'm not trying to [xkcd](https://xkcd.com/927/) a data format.  My goal here is to
produce and consume JSON.  This JSON can be run through any JSON parser in any language, it can be
stored and queried in a PostgreSQL database, it can easily be read with human eyeballs.  Yet,
there's a big wooden horse hiding arbitrary data types in plain sight...tagged values.

A tagged value is a map with a single key/value pair where the key is a tag.  For example:

```json
{"#set" : [1]}
```

*For more information see the [Argus "manifesto."](https://argus.fyi/)*

Tagged values are a good idea!  In the Clojur/Script world there are two popular data format with
tagged values (EDN and Transit), so one could reasonably ask why another library/format/etc.?  In
particular Argus overlaps with Transit.

### What argus does

- Encode local date, instant, set, and UUID values.
- Extend encoding for arbitrary types.
- Produce standard JSON values.
- Produce human readable values.

## What argus does not do

- Support arbitrary objects as map keys (keys must be keywords, symbols, or strings).
- Depend on Jackson and all its dependency nonsense
- Include a JSON reader/writer (you need to bring your own and could use Jackson if you like
  dependency nonsense)
- Deduplicate or compress any values (you can compress the resulting JSON, if you'd like)

## Examples

```clojure
user> (require '[systems.thoughtfull.argus :as argus])
nil
```

### Enargus

```clojure
user> (argus/enargus (argus/argus) #{1 2 3})
{"#set" [1 3 2]}
```

### Deargus

```clojure
user> (argus/deargus (argus/argus) {"#set" [1 3 2]})
#{1 3 2}
```

### Extending

```clojure
user> (defrecord CustomType [a b])
user.CustomType

user> (def a (argus/argus :encoders {CustomType ["#my/type" (juxt :a :b)]}
                          :decoders {"#my/type" (partial apply ->CustomType)}))
#'user/a

user> (argus/enargus a (->CustomType 1 2))
{"#my/type" [1 2]}

user> (argus/deargus a {"#my/type" [1 2]})
#user.CustomType{:a 1, :b 2}
```

## Testing

Run Clojure tests with:

```bash
clojure -X:test
```

Run ClojureScript tests with:

```bash
npm run test && node target/test.js
```

## License

> Copyright © technosophist
>
> This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
> the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
>
> This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public
> License, v. 2.0.
