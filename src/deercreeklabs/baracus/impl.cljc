(ns deercreeklabs.baracus.impl
  (:refer-clojure :exclude [byte-array])
  (:require
   [schema.core :as s]))

#?(:cljs
   (defn byte-array-cljs
     ([size-or-seq]
      (cond
        (sequential? size-or-seq)
        (byte-array-cljs (count size-or-seq) size-or-seq)

        (instance? js/Array size-or-seq)
        (byte-array-cljs (.-length ^js/Array size-or-seq) size-or-seq)

        (int? size-or-seq)
        (byte-array-cljs size-or-seq 0)

        :else
        (throw
         (ex-info (str "Argument to byte-array must be a sequence, "
                       "array, or an integer representing the size of "
                       "the array.")
                  {:arg size-or-seq}))))
     ([size init-val-or-seq]
      (let [ba (js/Int8Array. size)]
        (cond
          (sequential? init-val-or-seq)
          (.set ba (clj->js init-val-or-seq))

          (instance? js/Array init-val-or-seq)
          (.set ba init-val-or-seq)

          :else
          (.fill ba init-val-or-seq))
        ba))))

;; Make cljs byte-arrays countable
#?(:cljs
   (extend-protocol ICounted
     js/Int8Array
     (-count [this]
       (if this
         (.-length this)
         0))))

#?(:cljs
   (extend-protocol ICounted
     js/Uint8Array
     (-count [this]
       (if this
         (.-length this)
         0))))

;; Make cljs byte-arrays ISeqable
#?(:cljs
   (extend-protocol ISeqable
     js/Int8Array
     (-seq [o]
       (array-seq o))))

#?(:cljs
   (extend-protocol ISeqable
     js/Uint8Array
     (-seq [o]
       (array-seq o))))
