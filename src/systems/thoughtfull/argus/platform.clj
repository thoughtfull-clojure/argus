(ns ^:no-doc systems.thoughtfull.argus.platform
  (:require
    [systems.thoughtfull.argus.utils :refer [ident]])
  (:import
    (clojure.lang Ratio)
    (java.time ZoneOffset)
    (java.time.format DateTimeFormatter)))

(defn- find-base-encoder
  [cache c]
  (when (and c (not= Object c))
    (or (get @cache c)
      (find-base-encoder cache (Class/.getSuperclass c)))))

(defn- find-interface-encoder
  ([cache c]
   (find-interface-encoder cache c c nil (seq (Class/.getInterfaces c))))
  ([cache c b encoder [i & ii]]
   (if (and b (not= Object b))
     (if i
       (if-let [encoder' (get @cache i)]
         (do (when encoder (throw (ex-info "multiple encoders" {:class c})))
           (recur cache c b encoder' ii))
         (recur cache c b encoder ii))
       (let [b (Class/.getSuperclass b)]
         (find-interface-encoder cache c b encoder (Class/.getInterfaces b))))
     encoder)))

(defn find-encoder
  [cache c]
  (when-let [encoder (or (find-base-encoder cache c) (find-interface-encoder cache c))]
    (swap! cache assoc c encoder)
    encoder))

(def instant-formatter
  (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ss.SSSX"))

(defn- encode-instant
  [i]
  (.format instant-formatter (.atOffset i (ZoneOffset/of "Z"))))

(def default-encoders
  {clojure.lang.IPersistentSet (fn [o] {"#set" (vec o)})
   java.time.LocalDate (fn [o] {"#date" (str o)})
   java.time.Instant (fn [o] {"#instant" (encode-instant o)})
   java.util.Date (fn [o] (java.util.Date/.toInstant o))
   java.sql.Date (fn [o] (java.sql.Date/.toLocalDate o))
   java.sql.Timestamp (fn [o] (java.sql.Timestamp/.toInstant o))
   java.util.UUID (fn [o] {"#uuid" (str o)})
   clojure.lang.Keyword (fn [o] {"#clojure.keyword" (ident o)})
   clojure.lang.Symbol (fn [o] {"#clojure.symbol" (ident o)})
   clojure.lang.BigInt (fn [o] {"#clojure.bigint" (str o)})
   java.math.BigInteger (fn [o] {"#clojure.biginteger" (str o)})
   java.math.BigDecimal (fn [o] {"#clojure.bigdec" (str o)})
   Ratio (fn [^Ratio o] {"#clojure.ratio" [(.numerator o) (.denominator o)]})})

(def default-decoders
  {"#set" set
   "#date" java.time.LocalDate/parse
   "#instant" java.time.Instant/parse
   "#uuid" parse-uuid
   "#clojure.keyword" keyword
   "#clojure.symbol" symbol
   "#clojure.bigint" bigint
   "#clojure.biginteger" biginteger
   "#clojure.bigdec" bigdec
   "#clojure.ratio" (fn [[num denom]] (Ratio. num denom))})
