(ns deercreeklabs.doo-test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [deercreeklabs.baracus-test]))

(enable-console-print!)

(doo-tests 'deercreeklabs.baracus-test)
