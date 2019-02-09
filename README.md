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
Clojure & Clojurescript. This documentation is a work in progress.

## Project Name
This project works with `byte-array` data, which is often abbreviated
`ba` in code. [B.A. Baracus](https://en.wikipedia.org/wiki/B._A._Baracus)
was a character on the 1980s TV series
[The A-Team](https://en.wikipedia.org/wiki/The_A-Team).

# Examples
TBD

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
Tests if the argument is a byte array. For Clojure, this means a
Java primitive byte array. For ClojureScript, this means an instance
of the `js/Int8Array` data type.

#### Parameters
* `arg`: The argument to be tested

#### Return Value
`true` if the argument is a byte array, `false` otherwise

#### Example
```clojure
(require '[deercreeklabs.baracus :as ba])

(def my-ba (ba/byte-array 10))

(ba/byte-array? my-ba)
;; true

(ba/byte-array? "not a byte-array")
;; false
```
-------------------------------------------------------------------------------

## License

Distributed under the Apache Software License, Version 2.0
http://www.apache.org/licenses/LICENSE-2.0.txt

Copyright (c) 2017-2019 Deer Creek Labs, LLC
