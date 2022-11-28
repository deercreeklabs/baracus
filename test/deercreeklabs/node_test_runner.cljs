(ns deercreeklabs.node-test-runner
  (:require
   [cljs.nodejs :as nodejs]
   [cljs.test :as test :refer-macros [run-tests]]
   [deercreeklabs.baracus-test]))

(nodejs/enable-util-print!)

(defn -main [& _args]
  (run-tests 'deercreeklabs.baracus-test))

(set! *main-cli-fn* -main)
