(ns deercreeklabs.baracus
  (:refer-clojure :exclude [byte-array])
  (:require
   #?(:cljs [pako])
   #?(:cljs [goog.crypt :as gc])
   #?(:cljs [goog.crypt.base64 :as b64])
   [schema.core :as s])
  #?(:clj
     (:import
      (com.google.common.primitives Bytes)
      (java.io ByteArrayInputStream ByteArrayOutputStream)
      (java.util Arrays Base64)
      (java.util.zip DeflaterOutputStream InflaterOutputStream))))

#?(:cljs
   (set! *warn-on-infer* true))

;;;;;;;;;;;;;;;;;;;; Schemas ;;;;;;;;;;;;;;;;;;;;

(def Nil (s/eq nil))

(def ByteArray
  #?(:clj
     (class (clojure.core/byte-array []))
     :cljs
     js/Int8Array))

(def SizeOrSeq
  (s/if integer?
    s/Num
    [s/Any]))

;;;;;;;;;;;;;;;;;;;; byte-arrays ;;;;;;;;;;;;;;;;;;;;

#?(:cljs (def class type))

(s/defn byte-array? :- s/Bool
  [x :- s/Any]
  (when-not (nil? x)
    (boolean (= ByteArray (class x)))))

#?(:cljs
   (s/defn byte-array-cljs :- ByteArray
     ([size-or-seq :- SizeOrSeq]
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

(s/defn byte-array :- ByteArray
  ([size-or-seq :- SizeOrSeq]
   (#?(:clj clojure.core/byte-array
       :cljs byte-array-cljs) size-or-seq))
  ([size :- SizeOrSeq
    init-val-or-seq :- (s/if sequential?
                         [s/Any]
                         s/Any)]
   (#?(:clj clojure.core/byte-array
       :cljs byte-array-cljs) size init-val-or-seq)))

(s/defn concat-byte-arrays :- (s/maybe ByteArray)
  [arrays :- (s/maybe [(s/maybe ByteArray)])]
  (when arrays
    (let [arrays (keep identity arrays)]
      (case (count arrays)
        0 nil
        1 (first arrays)
        #?(:clj (Bytes/concat (into-array arrays))
           :cljs
           (let [len (reduce (fn [acc ba]
                               (+ acc (count ba)))
                             0 arrays)
                 ^js/Int8Array new-ba (byte-array len)]
             (reduce (fn [pos ba]
                       (.set new-ba ba pos)
                       (+ pos (count ba)))
                     0 arrays)
             new-ba))))))

(s/defn equivalent-byte-arrays? :- s/Bool
  [a :- ByteArray
   b :- ByteArray]
  (and
   (= (count a) (count b))
   (let [num (count a)]
     (loop [i 0]
       (if (>= i num)
         true
         (if (= (aget ^bytes a i)
                (aget ^bytes b i))
           (recur (int (inc i)))
           false))))))

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

(s/defn byte-array->debug-str :- s/Str
  [ba :- ByteArray]
  #?(:clj (let [s (map (fn [b]
                         (cond->> b
                           (neg? b) (+ 256)
                           true (str)))
                       ba)]
            (str "[" (clojure.string/join ", " s) "]"))
     :cljs (str ba)))

(s/defn slice-byte-array :- (s/maybe ByteArray)
  "Return a slice of the given byte array.
   Args:
        - ba - Byte array to be sliced. Required.
        - start - Start index. Optional. Defaults to 0.
        - end - End index. Optional. If not provided, the slice will extend
             to the end of the array. The returned slice will not contain
             the byte at the end index position, i.e.: the slice fn uses
             a half-open interval."
  ([ba :- (s/maybe ByteArray)]
   (when ba
     (slice-byte-array ba 0 (count ba))))
  ([ba :- (s/maybe ByteArray)
    start :- s/Num]
   (slice-byte-array ba start (count ba)))
  ([ba :- (s/maybe ByteArray)
    start :- s/Num
    end :- s/Num]
   (when (> start end)
     (throw (ex-info "Slice start is greater than end."
                     {:type :illegal-argument
                      :subtype :slice-start-is-greater-than-end
                      :start start
                      :end end})))
   (let [stop (min end (count ba))]
     #?(:clj
        (Arrays/copyOfRange ^bytes ba ^int start ^int stop)
        :cljs
        (.slice ^js/Int8Array ba start stop)))))

(s/defn reverse-byte-array :- (s/maybe ByteArray)
  "Returns a new byte array with bytes reversed."
  [ba :- (s/maybe ByteArray)]
  (when ba
    (let [num (count ba)
          last (dec num)
          new (byte-array num)]
      (dotimes [i num]
        (aset ^bytes new i ^byte (aget ^bytes ba (- last i))))
      new)))

#?(:clj
   (s/defn read-byte-array-from-file :- ByteArray
     [filename :- s/Str]
     (let [file ^java.io.File (clojure.java.io/file filename)
           result (byte-array (.length file))]
       (with-open [in (java.io.DataInputStream.
                       (clojure.java.io/input-stream file))]
         (.readFully in result))
       result)))

#?(:clj
   (s/defn write-byte-array-to-file :- Nil
     [filename :- s/Str
      ba :- ByteArray]
     (with-open [out (clojure.java.io/output-stream
                      (clojure.java.io/file filename))]
       (.write out ^bytes ba))
     nil))

(s/defn byte-array->fragments :- (s/maybe [ByteArray])
  [ba :- (s/maybe ByteArray)
   fragment-size :- s/Int]
  (when ba
    (if (zero? fragment-size)
      (slice-byte-array ba)
      (loop [offset 0
             output []]
        (if (>= offset (count ba))
          output
          (let [end-offset (+ offset fragment-size)
                fragment (slice-byte-array ba offset end-offset)]
            (recur (int end-offset)
                   (conj output fragment))))))))

(s/defn decode-int :- [(s/one s/Int :int)
                       (s/optional ByteArray :unread-remainder)]
  "Takes an zig-zag encoded byte array and reads an integer from it.
   Returns a vector of the integer and, optionally, any unread bytes."
  [ba :- ByteArray]
  (loop [n 0
         i 0
         out 0]
    (let [b (aget ^bytes ba n)]
      (if (zero? (bit-and b 0x80))
        (let [zz-n (-> (bit-shift-left b i)
                       (bit-or out))
              int-out (->> (bit-and zz-n 1)
                           (- 0)
                           (bit-xor (unsigned-bit-shift-right zz-n 1)))]
          (if (< (inc n) (count ba))
            (do
              [int-out (slice-byte-array ba (inc n))])
            [int-out]))
        (let [out (-> (bit-and b 0x7f)
                      (bit-shift-left i)
                      (bit-or out))
              i (+ 7 i)]
          (if (<= i 31)
            (recur (inc n) (int i) (int out))
            (throw
             (ex-info "Variable-length quantity is more than 32 bits"
                      {:type :illegal-argument
                       :subtype :var-len-num-more-than-32-bits
                       :i i}))))))))

(s/defn encode-int :- ByteArray
  "Zig zag encodes an integer. Returns the encoded bytes."
  [i :- s/Int]
  (let [zz-n (bit-xor (bit-shift-left i 1) (bit-shift-right i 31))]
    (loop [n zz-n
           out []]
      (if (zero? (bit-and n -128))
        (byte-array (conj out (bit-and n 0x7f)))
        (let [b (-> (bit-and n 0x7f)
                    (bit-or 0x80))]
          (recur (unsigned-bit-shift-right n 7)
                 (conj out b)))))))

(s/defn byte-array->b64 :- (s/maybe s/Str)
  [b :- (s/maybe ByteArray)]
  (when b
    #?(:clj
       (.encodeToString (Base64/getEncoder) b)
       :cljs
       (b64/encodeByteArray (js/Uint8Array. b)))))

(s/defn b64->byte-array :- (s/maybe ByteArray)
  [s :- (s/maybe s/Str)]
  (when s
    #?(:clj
       (.decode (Base64/getDecoder) ^String s)
       :cljs
       (-> (b64/decodeStringToUint8Array s)
           (js/Int8Array.)))))

(s/defn byte-array->utf8 :- (s/maybe s/Str)
  [ba :- (s/maybe ByteArray)]
  (when ba
    #?(:clj
       (String. #^bytes ba "UTF-8")
       :cljs
       (gc/utf8ByteArrayToString (js/Uint8Array. ba)))))

(s/defn utf8->byte-array :- (s/maybe ByteArray)
  [s :- (s/maybe s/Str)]
  (when s
    #?(:clj
       (.getBytes ^String s "UTF-8")
       :cljs
       (js/Int8Array. (gc/stringToUtf8ByteArray s)))))

(s/defn byte-array->hex-str :- (s/maybe s/Str)
  [ba :- (s/maybe ByteArray)]
  (when ba
    (let [len (count ba)
          hex-chars [\0 \1 \2 \3 \4 \5 \6 \7 \8 \9 \a \b \c \d \e \f]
          ca (#?(:clj char-array :cljs js/Array.) (* 2 len))]
      (dotimes [i len]
        (let [b (bit-and (aget ^bytes ba i) 0xff)
              j (* 2 i)]
          (#?(:clj aset-char :cljs aset) ca j (hex-chars (bit-shift-right b 4)))
          (#?(:clj aset-char :cljs aset) ca (inc j) (hex-chars
                                                     (bit-and b 0x0f)))))
      #?(:clj (String. ca)
         :cljs (.join ^js/Array ca "")))))

(defn char->int [ch]
  #?(:clj (Character/digit ^Character ch 16)
     :cljs (js/parseInt ch 16)))

(s/defn hex-str->byte-array :- (s/maybe ByteArray)
  [s :- (s/maybe s/Str)]
  (when s
    (let [ba-len (/ (count s) 2)
          ba (byte-array ba-len)]
      (dotimes [i ba-len]
        (let [j (* 2 i)]
          (aset ^bytes ba i
                (unchecked-byte
                 (+ (bit-shift-left (char->int (get s j)) 4)
                    (char->int (get s (inc j))))))))
      ba)))

#?(:cljs
   (defn signed-byte-array->unsigned-byte-array [ba]
     (when ba
       (js/Uint8Array. ba))))

#?(:cljs
   (defn unsigned-byte-array->signed-byte-array [ba]
     (when ba
       (js/Int8Array. ba))))

;;;;;;;;;;;;;;;;;;;; Compression / Decompression ;;;;;;;;;;;;;;;;;;;;

(s/defn deflate :- (s/maybe ByteArray)
  [data :- (s/maybe ByteArray)]
  (when data
    #?(:clj
       (let [os (ByteArrayOutputStream.)
             ds (DeflaterOutputStream. os)]
         (.write ^DeflaterOutputStream ds ^bytes data)
         (.close ds)
         (.toByteArray os))
       :cljs
       (->> data
            (signed-byte-array->unsigned-byte-array)
            (pako/deflate)
            (unsigned-byte-array->signed-byte-array)))))

(s/defn inflate :- (s/maybe ByteArray)
  [deflated-data :- (s/maybe ByteArray)]
  (when deflated-data
    #?(:clj
       (let [os (ByteArrayOutputStream.)
             infs (InflaterOutputStream. os)]
         (.write ^InflaterOutputStream infs ^bytes deflated-data)
         (.close infs)
         (.toByteArray os))
       :cljs
       (->> deflated-data
            (signed-byte-array->unsigned-byte-array)
            (pako/inflate)
            (unsigned-byte-array->signed-byte-array)))))
