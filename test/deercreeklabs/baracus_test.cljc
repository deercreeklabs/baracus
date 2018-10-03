(ns deercreeklabs.baracus-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [deercreeklabs.baracus :as ba]
   [schema.test :as st]))

(use-fixtures :once st/validate-schemas)

(defn get-current-time-ms []
  #?(:clj (System/currentTimeMillis)
     :cljs (.getTime (js/Date.))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Unit tests

(deftest test-byte-array
  (let [ba (ba/byte-array [1 2 3])
        _ (is (= 3 (aget ^bytes ba 2)))
        _ (is (ba/byte-array? ba))
        ba (ba/byte-array 4)
        _ (is (= 4 (count ba)))
        _ (is (zero? (aget ^bytes ba 2)))
        ba (ba/byte-array 3 [1 2])]
    (is (= 3 (count ba)))
    (is (= 1 (aget ^bytes ba 0)))
    (is (zero? (aget ^bytes ba 2)))))

(deftest test-concat-byte-arrays
  (let [ba1 (ba/byte-array [1 2 3])
        ba2 (ba/byte-array [4 5 6 7])
        ba3 (ba/byte-array [8 9])
        all (ba/concat-byte-arrays [ba1 ba2 ba3])]
    (is (ba/equivalent-byte-arrays? (ba/byte-array [1 2 3 4 5 6 7 8 9]) all))
    (is (nil? (ba/concat-byte-arrays [])))
    (is (nil? (ba/concat-byte-arrays nil)))))

(deftest test-slice
  (let [ba (ba/byte-array (range 10))
        s1 (ba/slice-byte-array ba 0 3)
        s2 (ba/slice-byte-array ba 3 5)
        s3 (ba/slice-byte-array ba 5)]
    (is (ba/equivalent-byte-arrays? (ba/byte-array (range 0 3)) s1))
    (is (ba/equivalent-byte-arrays? (ba/byte-array (range 3 5)) s2))
    (is (ba/equivalent-byte-arrays? (ba/byte-array (range 5 10)) s3))
    (is (ba/equivalent-byte-arrays? ba (ba/concat-byte-arrays [s1 s2 s3])))))

(deftest test-utf-conversions
  (let [data [[[1 2 3] "\1\2\3"]
              [[44 89 123 66 78] ",Y{BN"]
              [[206 186 225 189 185 207 131 206 188 206 181] "κόσμε"]]]
    (doseq [[a expected] data]
      (let [ba (ba/byte-array a)
            s (ba/byte-array->utf8 ba)
            rt (ba/utf8->byte-array s)]
        (is (= expected s))
        (is (ba/equivalent-byte-arrays? ba rt))))))
#?(:cljs
   (deftest test-signed-array-conversions
     (let [s (ba/byte-array [-1 2 -127])
           u (js/Uint8Array. s)
           s1 (ba/unsigned-byte-array->signed-byte-array u)
           u1 (ba/signed-byte-array->unsigned-byte-array s)]
       (is (= -1 (aget s 0)))
       (is (= -1 (aget s1 0)))
       (is (= 255 (aget u 0)))
       (is (= 255 (aget u1 0)))
       (is (ba/equivalent-byte-arrays?
            s (ba/unsigned-byte-array->signed-byte-array u))))))

(deftest test-deflate-inflate
  (let [data (ba/byte-array
              [1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1
               2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2])
        deflated (ba/deflate data)
        deflated-b64 (ba/byte-array->b64 deflated)
        _ (is (= "eJxjZMQNmPAAAAgKAFU=" deflated-b64))
        rt-data (ba/inflate deflated)]
    (is (ba/equivalent-byte-arrays? data rt-data))))

(deftest test-deflate-inflate-signed
  (let [data (ba/byte-array
              [-1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1
               -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1
               2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2])
        deflated (ba/deflate data)
        deflated-b64 (ba/byte-array->b64 deflated)
        _ (is (= "eJz7/58wYMIDAIc8JBU=" deflated-b64))
        rt-data (ba/inflate deflated)]
    (is (ba/equivalent-byte-arrays? data rt-data))))

(deftest test-deflate-inflate-data
  (let [data (ba/byte-array
              [36 116 101 115 116 101 109 97 105 108 64 116 101 115 116 46
               99 111 109 24 116 101 115 116 112 97 115 115 119 111 114 100])
        deflated (ba/deflate data)
        deflated-b64 (ba/byte-array->b64 deflated)
        _ (is (= "eJxTKUktLknNTczMcQCx9JLzcyVAjILE4uLy/KIUAMtfDKU="
                 deflated-b64))
        rt-data (ba/inflate deflated)]
    (is (ba/equivalent-byte-arrays? data rt-data))))

(deftest test-constructor-w-generic-arrays
  (let [data [1 3 5]
        ret (ba/byte-array #?(:clj (byte-array data)
                              :cljs (js/Array.from data)))]
    (is (ba/byte-array? ret))
    (is (ba/equivalent-byte-arrays? ret (ba/byte-array data)))))

(deftest test-byte-array->hex-str
  (let [data (ba/byte-array (range 100))
        expected (str "000102030405060708090a0b0c0d0e0f101112131415161718191a1b"
                      "1c1d1e1f202122232425262728292a2b2c2d2e2f3031323334353637"
                      "38393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f50515253"
                      "5455565758595a5b5c5d5e5f60616263")]
    (is (= expected (ba/byte-array->hex-str data)))))

(deftest test-hex-str->byte-array
  (let [hex-str (str "000102030405060708090a0b0c0d0e0f101112131415161718191a1b"
                     "1c1d1e1f202122232425262728292a2b2c2d2e2f3031323334353637"
                     "38393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f50515253"
                     "5455565758595a5b5c5d5e5f60616263")
        expected (ba/byte-array (range 100))]
    (is (ba/equivalent-byte-arrays?
         expected (ba/hex-str->byte-array hex-str)))))

(deftest test-sha256
  (let [ba (ba/utf8->byte-array "Hello World...")
        hash-hex (str "b49b84d7a9d40a621b26cdc2c5f7be74"
                      "c022b3a1e51146e28072aa97db59cd00")
        expected-ba (ba/hex-str->byte-array hash-hex)
        ret (ba/sha256 ba)]
    (is (ba/equivalent-byte-arrays? expected-ba ret))))
