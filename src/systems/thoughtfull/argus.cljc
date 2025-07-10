(ns systems.thoughtfull.argus
  (:require
    [systems.thoughtfull.argus.platform :refer [default-decoders default-encoders find-encoder]]
    [systems.thoughtfull.argus.utils :refer [ident]]))

#?(:clj (set! *warn-on-reflection* true))

(defn- tag?
  [o]
  (re-matches #"#([a-zA-Z0-9-_]+/)?[a-zA-Z0-9-_]+" (or (ident o) "")))

(defn- tagged-value?
  [o]
  (and (map? o) (= 1 (count o)) (tag? (key (first o)))))

(declare enargus*)

(defn- enargus-map
  [cache m]
  (if #?(:clj (instance? clojure.lang.IEditableCollection m) :cljs false)
    (-> (reduce-kv (fn [m k v] (assoc! m k (enargus* cache v))) (transient m) m)
      persistent!
      (with-meta (meta m)))
    (reduce-kv (fn [m k v] (assoc m k (enargus* cache v))) m m)))

(defn- enargus-vector
  [cache v]
  (into (empty v) (map (partial enargus* cache)) v))

(defn- enargus-object
  [cache o]
  (let [c (type o)]
    (if-let [encoder (find-encoder cache c)]
      (enargus* cache (encoder o))
      (throw (ex-info "missing encoder" {:class c})))))

(defn- enargus*
  [cache o]
  (cond
    (or (nil? o) (boolean? o) (integer? o) (double? o) (string? o))
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

(defn- deargus-map
  [argus m]
  (if-let [[t v] (tagged-value m)]
    (if-let [decoder (get-in argus [:decoders t])]
      (decoder (deargus argus v))
      m)
    (if #?(:clj (instance? clojure.lang.IEditableCollection m) :cljs false)
      (-> (reduce-kv (fn [m k v] (assoc! m k (deargus argus v))) (transient m) m)
        persistent!
        (with-meta (meta m)))
      (reduce-kv (fn [m k v] (assoc m k (deargus argus v))) m m))))

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

(defn- valid-tag
  [t]
  (when (nil? (second (tag? t)))
    (throw (ex-info "invalid extension tag" {:tag t})))
  (ident t))

(defn- ->encoder
  [encoder]
  (if (vector? encoder)
    (let [[t f] encoder
          t (valid-tag (ident t))]
      (fn [o] {t (f o)}))
    encoder))

(defn argus
  "Create an instance of argus that uses specified encoders and decoders for tagged values."
  [& {:keys [encoders decoders]}]
  (let [encoders' (merge default-encoders (zipmap (keys encoders) (map ->encoder (vals encoders))))]
    {:cache (atom encoders')
     :encoders encoders'
     :decoders (merge default-decoders (zipmap (map valid-tag (keys decoders)) (vals decoders)))}))
