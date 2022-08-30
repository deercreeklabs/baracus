(ns deercreeklabs.baracus.utils
  (:import
   #?(:clj (java.util Date)
      :cljs (goog.math Long)))
  #?(:cljs
     (:require-macros
      [deercreeklabs.baracus.utils :refer [sym-map]])))

;;;;;;;;; Macros  ;;;;;;;;;

(defmacro sym-map
  "Builds a map from symbols.
   Symbol names are turned into keywords and become the map's keys.
   Symbol values become the map's values.
  (let [a 1
        b 2]
    (sym-map a b))  =>  {:a 1 :b 2}"
  [& syms]
  (zipmap (map keyword syms) syms))

;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn date-time->date-ms [dt]
  #?(:cljs (if (instance? js/Date dt)
             (.fromNumber ^Long Long (.getTime dt))
             (throw (ex-info (str "`dt` argument must be a js/Date. Got: `"
                                  dt "`.")
                             (sym-map dt))))
     :clj  (if (instance? Date dt)
             (.getTime ^Date dt)
             (throw (ex-info (str "`dt` argument must be a java.util.Date. "
                                  "Got: `" dt "`.")
                             (sym-map dt))))))

(defn current-time-ms []
  #?(:clj (System/currentTimeMillis)
     :cljs (date-time->date-ms (js/Date.))))
