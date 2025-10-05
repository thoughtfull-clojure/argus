(ns systems.thoughtfull.argus
  (:require
    [systems.thoughtfull.argus.platform :refer [default-decoders default-encoders find-encoder]]
    [systems.thoughtfull.argus.utils :refer [ident]]))

#?(:clj (set! *warn-on-reflection* true))

(defn- tag?
  [k]
  (= \# (first (or (ident k) ""))))

(defn- tagged-value?
  [o]
  (and (map? o) (= 1 (count o)) (tag? (key (first o)))))

(declare enargus*)

(defn- enargus-key
  "If a k is as string beginning with a colon or quote then escape it, otherwise return a string
  representation of k."
  [k]
  (if (and (string? k) (pos? (count k)))
    (let [f (subs k 0 1)]
      (cond
        (= ":" f) (str ":" k)
        (= "'" f) (str "'" k)
        :else k))
    (if (symbol? k)
      (str "'" k)
      (str k))))

(defn- enargus-map
  "Encode objects from the inside out.  Map keys are encoded specially so strings, symbols, and
  keywords can be distinguished (a keyword starts with a colon, a symbol starts with a quote, a
  string starting with a colon or quote is escaped)."
  [cache m]
  (if #?(:clj (instance? clojure.lang.IEditableCollection m) :cljs false)
    (-> (reduce-kv
          (fn [m k v]
            (let [k' (enargus-key k)]
              (cond-> (assoc! m k' (enargus* cache v))
                (not= k k') (dissoc! k))))
          (transient m)
          m)
      persistent!
      (with-meta (meta m)))
    (reduce-kv
      (fn [m k v]
        (let [k' (enargus-key k)]
          (cond-> (assoc m k' (enargus* cache v))
            (not= k k') (dissoc k))))
      m
      m)))

(defn- enargus-vector
  [cache v]
  (into (empty v) (map (partial enargus* cache)) v))

(defn- enargus-object
  [cache o]
  (let [c (type o)]
    (if-let [encoder (find-encoder cache c)]
      (enargus* cache (encoder o))
      (throw (ex-info "missing encoder" {:type c})))))

(defn- enargus*
  [cache o]
  (cond
    (or (nil? o) (boolean? o) (int? o) (double? o) (string? o))
    o
    (and (map? o) (not (record? o)))
    (enargus-map cache o)
    (vector? o)
    (enargus-vector cache o)
    :else
    (enargus-object cache o)))

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

  -**`argus`** — an argus specification produced by [[argus]].
  -**`value`** — a JSON-compatible tagged value to encode."
  [argus value]
  (enargus* (:cache argus) value))

(declare deargus)

(defn- tagged-value
  [o]
  (when (tagged-value? o)
    (first o)))

(defn- deargus-key
  [k]
  (if (and (string? k) (> (count k) 1))
   (cond
     (or (= "::" (subs k 0 2)) (= "''" (subs k 0 2)))
     (subs k 1)
     (= ":" (subs k 0 1))
     (keyword (subs k 1))
     (= "'" (subs k 0 1))
     (symbol (subs k 1))
     :else
     k)
   k))

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
  [argus m]
  (if-let [[t v] (tagged-value m)]
    (let [v' (deargus argus v)]
      (if-some [decoder (get-in argus [:decoders t])]
        (decoder v')
        (let [decoder (or (:default-decoder argus)
                        default-decoder)]
          (decoder t v'))))
    (if #?(:clj (instance? clojure.lang.IEditableCollection m) :cljs false)
      (-> (reduce-kv
            (fn [m k v]
              (let [k' (deargus-key k)]
                (cond-> (assoc! m k' (deargus argus v))
                  (not= k k') (dissoc! m k))))
            (transient m)
            m)
        persistent!
        (with-meta (meta m)))
      (reduce-kv
        (fn [m k v]
          (let [k' (deargus-key k)]
            (cond-> (assoc m k' (deargus argus v))
              (not= k k') (dissoc m k))))
        m
        m))))

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

  -**`argus`** — an argus specification produced by [[argus]].
  -**`value`** — a JSON-compatible tagged value to decode."
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
    (when (nil? (tag-namespace t'))
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
        (valid-encoder-tag (key (first e)))
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

  Example:

  ```clojure
  systems.thoughtfull.argus> (argus :encoders {MyType [\"#my.type\" my-type-encoder]}
                               :decoders {\"#my.type\" my-type-decoder}
                               :default-decoder (fn [t v] :unknown-value))
  ```

  - **`encoders`** (optional) — a map from type to encoder.
  - **`decoders`** (optional) — a map from extension tag to decoder.
  - **`default-decoder`** (optional) — a two-argument function, defaults to `default-decoder`."
  [& {:keys [encoders decoders default-decoder]}]
  (let [encoders' (merge default-encoders (zipmap (keys encoders) (map ->encoder (vals encoders))))]
    {:cache (atom encoders')
     :encoders encoders'
     :decoders (merge (zipmap (map valid-decoder-tag (keys decoders)) (vals decoders))
                 default-decoders)
     :default-decoder default-decoder}))
