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
  "If a k is as string beginning with a colon then escape it, otherwise return the string of k."
  [k]
  (if (and (string? k)
        (= ":" (subs k 0 1)))
    (str ":" k)
    (str k)))

(defn- enargus-map
  "Encode objects from the inside out.  Map keys are encoded specially so strings and keywords can
  be distinguished (a keyword starts with a colon, a string starting with a colon is escaped)."
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
  "Rewrite o as JSON with tagged values according to the encoders in the given argus instance."
  [argus o]
  (enargus* (:cache argus) o))

(declare deargus)

(defn- tagged-value
  [o]
  (when (tagged-value? o)
    (first o)))

(defn- deargus-key
  [k]
  (if (string? k)
   (cond
     (= "::" (subs k 0 2))
     (subs k 1)
     (= ":" (subs k 0 1))
     (keyword (subs k 1))
     :else
     k)
   k))

(defn- deargus-map
  [argus m]
  (if-let [[t v] (tagged-value m)]
    (let [v' (deargus argus v)]
      (if-some [decoder (get-in argus [:decoders t])]
        (decoder v')
        (let [default-decoder (or (:default-decoder argus)
                                (fn [t v]
                                  {t v}))]
          (default-decoder t v'))))
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
  "Rewrite o as Clojure/Java values according to the decoders in the given argus instance."
  [argus o]
  (cond
    (map? o)
    (deargus-map argus o)
    (vector? o)
    (deargus-vector argus o)
    :else
    o))

(def tag-re
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
  "Create an instance of argus that uses specified encoders and decoders for tagged values.
  Encoders maps from type to either a function taking a single argument (the value) and returning
  a tagged value or a vector with a tag and a function that takes a single argument (the value)
  and returns the encoded value.

  Decoders maps from a tag to a function taking a single value (the encoded value) and returns the
  decoded value."
  [& {:keys [encoders decoders default-decoder]}]
  (let [encoders' (merge default-encoders (zipmap (keys encoders) (map ->encoder (vals encoders))))]
    {:cache (atom encoders')
     :encoders encoders'
     :decoders (merge default-decoders
                 (zipmap (map valid-decoder-tag (keys decoders)) (vals decoders)))
     :default-decoder default-decoder}))
