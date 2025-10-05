# argus

<img align="left" src="./img/argus-logo-150.png" />

[![Lint and test](https://github.com/thoughtfull-clojure/argus/actions/workflows/lint-and-test.yml/badge.svg)](https://github.com/thoughtfull-clojure/argus/actions/workflows/lint-and-test.yml) [![clojars badge](https://badgen.net/badge/clojars/0.3.2/blue)](https://clojars.org/systems.thoughtfull/argus/versions/0.3.2) [![cljdoc badge](https://badgen.net/badge/cljdoc/0.3.2/blue)](https://cljdoc.org/d/systems.thoughtfull/argus/0.3.2/doc/readme)

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

Tagged values are a good idea!  This is not an entirely new idea.  MongoDB has an [Extended
JSON](https://www.mongodb.com/docs/manual/reference/mongodb-extended-json/#std-label-mongodb-extended-json-v2)
format.  DynamoDB has [Data Type
Descriptors](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypeDescriptors).
Neither of these are user extensible and available as external an library.

The [jsonista](https://github.com/metosin/jsonista/tree/master?tab=readme-ov-file#tagged-json)
library has an object mapper for reading and writing arbitrary tagged JSON data, but it is a
JVM-only library.

In the Clojure/Script world there are two other ways to use tagged values (EDN and Transit), so one
could reasonably ask why another library/format/etc.?

In particular argus overlaps with Transit.  Like Transit, argus embraces JSON for its universality.
And like Transit, argus tags data and is extensible.

### What argus does

- En/decode local date, instant, set, and UUID values.
- Extend en/decoding for arbitrary types.
- Transparently en/decode keywords, symbols, and strings as map keys.
- Produce standard JSON values.
- Produce human readable values.

### What argus does NOT do

- Support arbitrary objects as map keys (keys must be keywords, symbols, or strings).
- Depend on Jackson and all its dependency nonsense
- Include a JSON reader/writer (you need to bring your own and could use Jackson if you like
  dependency nonsense)
- Deduplicate or compress any values (you can compress the resulting JSON, if you'd like)

### Summary

argus technically isn't even a JSON library.  It just rewrites Clojure data to Clojure data that is restricted to valid JSON values.

It works with both Clojure and ClojureScript, so it is suitable for sending rich data to and from backend and frontend.

## Do not keyword-ize map keys

***Because argus transparently handles keywords, symbols, and strings as map keys, I do not
recommend that you have your JSON library automatically convert map keys into keywords.  This will
interfere with the map key decoding process, and there are some cases where it is useful to maintain
string keys in data (for example, when dealing—not with "objects"—but "mappings" from one name to
another).***

## Performance

I wouldn't say that performance is not a concern, but my top priorities are:

1. Extensibility
2. Human readability
3. Support for both Clojure and ClojureScript

That said, I want argus to be performant, and I have tried to make it performant when I can.  If you
have performance improvements, I'm happy to take them.

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
