# baracus

![BA Baracus](doc/BABaracus.jpg)

* [Installation](#installation)
* [About](#about)
* [Examples](#examples)
* [Data Types](#data-types)
* [API Documentation](#api-documentation)
* [License](#license)

Utilities for working with byte arrays in Clojure & Clojurescript

## Installation
Using Leiningen / Clojars:

[![Clojars Project](http://clojars.org/deercreeklabs/baracus/latest-version.svg)](http://clojars.org/deercreeklabs/baracus)

# About
Baracus provides various utilities for working with byte arrays in
Clojure & Clojurescript.

## Project Name
This project works with `byte-array` data, which is often abbreviated
`ba` in code. [B.A. Baracus](https://en.wikipedia.org/wiki/B._A._Baracus)
was a character on the 1980s TV series
[The A-Team](https://en.wikipedia.org/wiki/The_A-Team).

# Examples


# Data Types
In Clojure, Baracus uses Java's primitive byte array. In ClojureScript,
Baracus uses the `js/Int8Array` data type. Note that bytes are signed
in both cases, following Java's implementation.

# API Documentation
All public vars and functions are in the `deercreeklabs.baracus` namespace.
Any other namespaces should be considered private implementation details
that may change.

-------------------------------------------------------------------------------
### byte-array?
```clojure
(byte-array? arg)
```
Creates default data that conforms to the given Lancaster schema. The following
values are used for the primitive data types:
* `null`: `nil`
* `boolean`: `false`
* `int`: `-1`
* `long`: `-1`
* `float`: `-1.0`
* `double`: `-1.0`
* `string`: `""`
* `enum`: first symbol in the schema's symbols list

Default data for complex schemas are built up from the primitives.

#### Parameters
* `schema`: The Lancaster schema

#### Return Value
Data that matches the given schema

#### Example
```clojure
(l/def-enum-schema suite-schema
  :clubs :diamonds :hearts :spades)

(l/default-data suite-schema)
;; :clubs
```
-------------------------------------------------------------------------------

## License

Distributed under the Apache Software License, Version 2.0
http://www.apache.org/licenses/LICENSE-2.0.txt

Copyright (c) 2017-2019 Deer Creek Labs, LLC
