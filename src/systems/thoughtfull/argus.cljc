(ns systems.thoughtfull.argus
  (:require
    [clojure.string :as str]
    [systems.thoughtfull.argus.platform :refer [default-decoders default-encoders find-encoder]]
    [systems.thoughtfull.argus.utils :refer [ident]]))

#?(:clj (set! *warn-on-reflection* true))

(defn tag?
  "True if k is an identifier (string, symbol, or keyword) starting with '#'."
  [k]
  (= \# (first (or (ident k) ""))))

(defn tagged-value?
  "True if o is a map with a single key/value pair where the key is a tag.

  See [[tag?]]"
  [o]
  (and (map? o) (= 1 (count o))
    (tag?
      ;; get the first key without creating a seq
      (reduce-kv (fn [_m k _v] (reduced k)) nil o))))

(declare enargus*)

(defn enargus-key
  "Encode a map key into a JSON string.

  If a key is a keyword, then it is encoded as a string starting with a colon.  Keyword namespaces
  are preserved.

  If a key is a symbol, then it is encoded as a string starting with a quote.  Symbol namespaces
  are preserved.

  If a key is a string starting with a colon or quote, then the colon or quote is escaped by
  doubling it.

  If given, write-key transforms a string key (minus the leading ':' for keywords) into a string
  for output."
  ([k]
   (enargus-key k identity))
  ([k write-key]
   (if (and (string? k) (pos? (count k)))
     (let [f (subs k 0 1)]
       (cond
         (= ":" f) (str ":" (write-key k))
         (= "'" f) (str "'" (write-key k))
         :else (write-key k)))
     (cond
       (symbol? k)
       (str "'" (write-key (str k)))
       (keyword? k)
       (str ":" (write-key (subs (str k) 1)))
       :else
       (str (write-key k))))))

(defn- enargus-complex-key-map
  "Encode objects from the inside out.  Map keys are encoded specially so strings, symbols, and
  keywords can be distinguished (a keyword starts with a colon, a symbol starts with a quote, a
  string starting with a colon or quote is escaped)."
  [find-encoder write-key m]
  (transient
    {"#clojure.map"
     (persistent!
       (reduce-kv
         (fn [m k v]
           (conj! m [(enargus* find-encoder write-key k) (enargus* find-encoder write-key v)]))
         (transient [])
         m))}))

(defn- enargus-map
  "Encode objects from the inside out.  Map keys are encoded specially so strings, symbols, and
  keywords can be distinguished (a keyword starts with a colon, a symbol starts with a quote, a
  string starting with a colon or quote is escaped)."
  [find-encoder write-key m]
  (-> (reduce-kv
        (fn [m' k v]
          (if (not (or (keyword? k) (symbol? k) (string? k)))
            (reduced (enargus-complex-key-map find-encoder write-key m))
            (let [k' (enargus-key k write-key)]
              (cond-> (assoc! m' k' (enargus* find-encoder write-key v))
                (not= k k') (dissoc! k)))))
        (transient (hash-map))
        m)
    persistent!
    (with-meta (meta m))))

(defn- enargus-sequential
  [find-encoder write-key v]
  (->
    (into [] (map (partial enargus* find-encoder write-key)) v)
    (with-meta (meta v))))

(defn- tagged-value
  [o]
  (when (tagged-value? o)
    (first o)))

(defn- enargus*
  [find-encoder write-key o]
  (cond
    ;; JavaScript... weird but true
    (or (nil? o) (boolean? o) (and (double? o) (not (int? o))) (string? o))
    o
    (and (int? o) (<= -9007199254740991 o 9007199254740991))
    o
    (int? o)
    {"#integer" (str o)}
    (tagged-value? o)
    (let [[t v] (tagged-value o)]
      {(str "#" t) (enargus* find-encoder write-key v)})
    :else
    (let [c (type o)
          encoder (find-encoder c)]
      (cond
        encoder
        (let [v (encoder o)]
          (if (tagged-value? v)
            (let [[t v] (tagged-value v)]
              {t (enargus* find-encoder write-key v)})
            (enargus* find-encoder write-key v)))
        (and (map? o) (not (record? o)))
        (enargus-map find-encoder write-key o)
        (sequential? o)
        (enargus-sequential find-encoder write-key o)
        :else
        (throw (ex-info "missing encoder" {:type c}))))))

(defn enargus
  "Encode arbitrary Clojure values into JSON-compatible tagged values.

  In addition keys in a map have a special encoding.  If a key is a keyword, then it is encoded as
  a string starting with a colon.  Keyword namespaces are preserved.

  If a key is a symbol, then it is encoded as a string starting with a quote.  Symbol namespaces
  are preserved.

  If a key is a string starting with a colon or quote, then the colon or quote is escaped by
  doubling it.

  Example:

  ```clojure
  systems.thoughtfull.argus> (enargus (argus) {:key/word #{1}
                                               'sym/bol #{2}
                                               \":not-keyword\" #{3}
                                               \"'not-symbol\" #{4}})
  {\":key/word\" {\"#set\" [1]},
   \"'sym/bol\" {\"#set\" [2]},
   \"::not-keyword\" {\"#set\" [3]},
   \"''not-symbol\" {\"#set\" [4]}}
  ```

  - **`argus`** — an argus specification produced by [[argus]].
  - **`value`** — a Clojure value to encode."
  [argus value]
  (enargus* (:find-encoder argus) (:write-key argus) value))

(declare deargus)

(defn deargus-key
  "Decode a string into an appropriate map key.

  If a key is a string starting with a colon, then it is decoded as a keyword.  Keyword namespaces
  are preserved.

  If a key is a string starting with a quote, then it is decoded as a symbol.  Symbol namespaces
  are preserved.

  If a key is a string starting with two colons or two quotes, then one colon or quote is removed
  and the rest of the string is returned unmodified.

  If given, read-key transforms a string key into a string for Clojure before it is converted into
  a keyword, symbol, etc."
  ([k]
   (deargus-key k identity))
  ([k read-key]
   (if (and (string? k) (> (count k) 1))
     (cond
       (or (= "::" (subs k 0 2)) (= "''" (subs k 0 2)))
       (read-key (subs k 1))
       (= ":" (subs k 0 1))
       (keyword (read-key (subs k 1)))
       (= "'" (subs k 0 1))
       (symbol (read-key (subs k 1)))
       :else
       (read-key k))
     k)))

(defn default-decoder
  "Takes a tag and value and returns a tagged value.

  Example:

  ```clojure
  user> (default-decoder \"#foo.bar\" 42)
  {\"#foo.bar\" 42}
  ```"
  [tag value]
  {tag value})

(defn- deargus-map
  [{:as argus :keys [read-key]} m]
  (if-let [[t v] (tagged-value m)]
    (let [v' (deargus argus v)]
      (if (str/starts-with? t "##")
        {(subs t 1) v'}
        (if-some [decoder (-> argus :decoders (get t))]
          (try
            (decoder v')
            (catch #?(:clj Exception :cljs js/Object) _
              m))
          (let [decoder (or (:default-decoder argus)
                          default-decoder)]
            (decoder t v')))))
    (-> (reduce-kv
          (fn [m k v]
            (let [k' (deargus-key k read-key)]
              (cond-> (assoc! m k' (deargus argus v))
                (not= k k') (dissoc! m k))))
          (transient m)
          m)
      persistent!
      (with-meta (meta m)))))

(defn- deargus-vector
  [argus v]
  (into (empty v) (map (partial deargus argus)) v))

(defn deargus
  "Decode JSON-compatible tagged values into arbitrary Clojure values.

  In addition keys in a map are decoded specially.  If a key is a string starting with a colon,
  then it is decoded as a keyword.  Keyword namespaces are preserved.

  If a key is a string starting with a quote, then it is decoded as a symbol.  Symbol namespaces
  are preserved.

  If a key is a string starting with two colons or two quotes, then one colon or quote is removed
  and the rest of the string is returned unmodified.

  *Because argus transparently handles keywords, symbols, and strings as map keys, I do not
  recommend that you have your JSON library automatically convert map keys into keywords.  This
  will interfere with the map key decoding process, and there are some cases where it is useful to
  maintain string keys in data (for example, when dealing—not with \"objects\"—but \"mappings\"
  from one name to another).*

  Example:

  ```clojure
  systems.thoughtfull.argus> (deargus (argus) {\":key/word\" {\"#set\" [1]},
                                               \"'sym/bol\" {\"#set\" [2]},
                                               \"::not-keyword\" {\"#set\" [3]},
                                               \"''not-symbol\" {\"#set\" [4]}})
  {:key/word #{1}, sym/bol #{2}, \":not-keyword\" #{3}, \"'not-symbol\" #{4}}
  ```

  - **`argus`** — an argus specification produced by [[argus]].
  - **`value`** — a JSON-compatible tagged value to decode."
  [argus value]
  (cond
    (map? value)
    (deargus-map argus value)
    (vector? value)
    (deargus-vector argus value)
    :else
    value))

(def ^:private tag-re
  #"#((?:[a-zA-Z0-9-_]+\.)*(?:[a-zA-Z0-9-_]+))\.[a-zA-Z0-9-_]+")

(defn- tag-namespace
  [t]
  (second (re-matches tag-re t)))

(defn- valid-encoder-tag
  [t]
  (let [t' (ident t)]
    (when (and (nil? (tag-namespace t')) (not (contains? #{"#set" "#date" "#instant" "#uuid"} t')))
      (throw (ex-info "invalid extension tag" {:tag t})))
    t'))

(defn- ->encoder
  [encoder]
  (if (vector? encoder)
    (let [[t f] encoder
          t (valid-encoder-tag (ident t))]
      (fn [o] {t (f o)}))
    (fn [o]
      (let [e (encoder o)]
        (when (tagged-value? e) (valid-encoder-tag (key (first e))))
        e))))

(defn- valid-decoder-tag
  [t]
  (let [t' (ident t)]
    (when (not (tag? t'))
      (throw (ex-info "invalid extension tag" {:tag t})))
    t'))

(defn argus
  "Create a specification for encoding/decoding arbitrary Clojure values into JSON-compatible values
  and tagged values (a map with a single key/value pair where the key is a valid tag).  A
  specification is thread safe.  Because it caches lookups reusing a specification improves
  performance and is encouraged.

  Every specification will have encoders and decoders for the standard argus tags: #date, #instant,
  #set, #uuid.

  A specification may be extended with encoders and decoders for arbitrary types.  Each encoder
  and decoder is associated with an extension tag.  An extension tag must begin with an octothorpe
  and contain at least two name segments separated by a period.  A name segment may contain
  alphanumerics, dash, and underscore.  For example: '#clojure.keyword'.

  Encoding is dispatched on a type.  If no suitable encoder can be found for a dispatch type,
  argus will look for an encoder for a superclass or interface, and if found it will cache the
  encoder for the dispatch type.  If a suitable encoder still cannot be found, then an error is
  thrown.

  An encoder can have two forms.  The first form is a vector of two elements the first being a
  valid extension tag and the second element being a single-argument function to encode an
  object. The second encoder form is a single-argument function taking an object and returning a
  tagged value.

  The first encoder form, the vector form, the preferred form, will have its tag validated once
  upon construction of the specification.  If the tag is not a valid extension tag, then an error
  is thrown.

  The second encoder form, the function form, will be wrapped in a function that validates the tag
  of every tagged value produced.  If the tag produced is not a valid extension tag, then an error
  is thrown.

  argus only validates tags and does not validate that encoded values are JSON compatible.  This
  is up to you and/or the JSON encoding library you use.

  Decoding is dispatched on a tag.  If no suitable decoder can be found for the tag, then a
  default decoder, if specified, is used.  A decoder is a single-argument function taking an
  already decoded value from which it produces a value of an arbitrary non-JSON-compatible type.

  The default decoder is a two-argument function taking the tag and an already decoded value from
  which it produces a value of an arbitrary non-JSON-compatible type.  If no default decoder is
  specified, then the default default decoder will return a tagged value.

  Each decoder tag in the specification is validated, and if a tag is not a valid extension tag,
  then an error is thrown.

  If you'd like to do kebab-case<->snake_case conversions (or other key transformations), use
  write-key and read-key.

  Example:

  ```clojure
  systems.thoughtfull.argus> (argus :encoders {MyType [\"#my.type\" my-type-encoder]}
                               :decoders {\"#my.type\" my-type-decoder}
                               :default-decoder (fn [t v] :unknown-value))
  ```

  - **`encoders`** (optional) — a map from type to encoder.
  - **`decoders`** (optional) — a map from extension tag to decoder.
  - **`default-decoder`** (optional) — a two-argument function, defaults to `default-decoder`.
  - **`write-key`** (optional) — a one-argument string-to-string function taking a Clojure map key
    as a string (minus the leading ':' for keywords) and transforming it for output, defaults to
    identity.
  - **`read-key`** (optional) — a one-argument string-to-string function taking a map key string and
    transforming it for use in Clojure, defaults to identity."
  [& {:keys [encoders decoders default-decoder read-key write-key]}]
  (let [encoders (merge default-encoders (zipmap (keys encoders) (map ->encoder (vals encoders))))]
    {:encoders encoders
     :decoders (merge
                 {"#set" #(if (vector? %) (set %) {"#set" %})
                  "#uuid" #(if-let [u (parse-uuid %)] u {"#uuid" %})
                  "#clojure.keyword" #(if-let [k (keyword %)] k {"#clojure.keyword" %})
                  "#clojure.list" (partial apply list)
                  "#clojure.map" (partial into {})
                  "#clojure.symbol" symbol}
                 default-decoders
                 (zipmap (map valid-decoder-tag (keys decoders)) (vals decoders)))
     :default-decoder default-decoder
     :find-encoder (memoize (partial find-encoder encoders))
     :read-key (or read-key identity)
     :write-key (or write-key identity)}))
