(ns ^:no-doc systems.thoughtfull.argus.platform
  (:require
    [systems.thoughtfull.argus.utils :refer [ident]])
  (:import
    (clojure.lang Ratio)
    (java.time ZoneOffset)
    (java.time.format DateTimeFormatter)))

(set! *warn-on-reflection* true)

(defn- find-base-encoder
  [encoders c]
  (if (and c (not= Object c))
    (if (contains? encoders c)
      (get encoders c)
      (find-base-encoder encoders (Class/.getSuperclass c)))
    ::missing))

(defn- find-interface-encoder
  ([encoders c]
   (find-interface-encoder encoders c c nil (Class/.getInterfaces c)))
  ([encoders c b encoder ^objects ii]
   (let [len (alength ii)]
     (loop [encoder encoder
            j 0]
       (if (and b (not= Object b))
         (if (< j len)
           (if-let [encoder' (get encoders (aget ii j))]
             (do (when encoder (throw (ex-info "multiple encoders" {:class c})))
               (recur encoder' (inc j)))
             (recur  encoder (inc j)))
           (let [b (Class/.getSuperclass b)]
             (find-interface-encoder encoders c b encoder (Class/.getInterfaces b))))
         encoder)))))

(defn find-encoder
  [encoders c]
  (let [encoder (find-base-encoder encoders c)]
    (if (= encoder ::missing)
      (find-interface-encoder encoders c)
      encoder)))

(def ^DateTimeFormatter instant-formatter
  (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ss.SSSX"))

(defn- encode-instant
  [^java.time.Instant i]
  (.format instant-formatter (.atOffset i (ZoneOffset/of "Z"))))

(def default-encoders
  {clojure.lang.IPersistentSet (fn [o] {"#set" (vec o)})
   ;; for map and vector, go immediately to implicit encoding
   clojure.lang.IPersistentMap nil
   clojure.lang.PersistentVector nil
   java.time.LocalDate (fn [o] {"#date" (str o)})
   java.time.Instant (fn [o] {"#instant" (encode-instant o)})
   java.util.Date (fn [o] (java.util.Date/.toInstant o))
   java.sql.Date (fn [o] (java.sql.Date/.toLocalDate o))
   java.sql.Timestamp (fn [o] (java.sql.Timestamp/.toInstant o))
   java.util.UUID (fn [o] {"#uuid" (str o)})
   Ratio (fn [^Ratio o] {"#clojure.ratio" [(.numerator o) (.denominator o)]})
   clojure.lang.BigInt (fn [o] {"#clojure.bigint" (str o)})
   clojure.lang.Keyword (fn [o] {"#clojure.keyword" (ident o)})
   clojure.lang.PersistentList (fn [o] {"#clojure.list" (vec o)})
   clojure.lang.PersistentQueue (fn [o] {"#clojure.queue" (vec o)})
   clojure.lang.Symbol (fn [o] {"#clojure.symbol" (ident o)})
   java.math.BigDecimal (fn [o] {"#clojure.bigdec" (str o)})
   java.math.BigInteger (fn [o] {"#clojure.biginteger" (str o)})})

(def default-decoders
  {"#set" set
   "#date" java.time.LocalDate/parse
   "#instant" java.time.Instant/parse
   "#integer" #(or (parse-long %) (bigint %))
   "#uuid" parse-uuid
   "#clojure.bigdec" bigdec
   "#clojure.bigint" bigint
   "#clojure.biginteger" biginteger
   "#clojure.keyword" keyword
   "#clojure.list" (partial apply list)
   "#clojure.map" (partial into {})
   "#clojure.queue" (partial into clojure.lang.PersistentQueue/EMPTY)
   "#clojure.ratio" (fn [[num denom]] (Ratio. num denom))
   "#clojure.symbol" symbol})
