# Argus

<img align="left" src="./img/argus-logo-150.png" />

[![Lint and test](https://github.com/thoughtfull-clojure/argus/actions/workflows/lint-and-test.yml/badge.svg)](https://github.com/thoughtfull-clojure/argus/actions/workflows/lint-and-test.yml) [![clojars badge](https://badgen.net/badge/clojars/0.1.0/blue)](https://clojars.org/systems.thoughtfull/argus/versions/0.1.0) [![cljdoc badge](https://badgen.net/badge/cljdoc/0.1.0/blue)](https://cljdoc.org/d/systems.thoughtfull/argus/0.1.0/doc/readme)

Extended and extensible types for JSON.  An implementation of [Argus](https://argus.fyi/).

JSON is a popular, but anemic data format.  It supports four scalar types and two container types.  This is a far cry from the richness of the real world of programming languages, and yet ... it is a robust and universal foundation if only it supported tagged values.

## Baseline versions

- JVM: 21
- Node: 20
- Clojure: 1.12.0

## Surprise! Tagged Values

Tagged values are a good idea!  In the Clojure world there are two popular data format with tagged values (EDN and Transit), so one could reasonably ask why another library/format/etc.?  In particular Argus overlaps heavily with Transit.

Argus and Transit both have tagged values and can be extended with new tags, both serialize to JSON.  The difference is Argus sticks closer to JSON by not supporting arbitrary objects as keys in a map and Argus is explicitly meant to be human readable.

Argus also does not depend on any particular JSON library, so no more Jackson dependency nonsense.  Argus translates Clojure data into JSON compatible tagged Clojure values which can the library of your choosing can serialize to JSON.

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

## License

> Copyright © technosophist
>
> This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
> the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
>
> This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public
> License, v. 2.0.
