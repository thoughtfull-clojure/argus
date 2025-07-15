(ns ^:no-doc systems.thoughtfull.argus.platform
  (:require
    [systems.thoughtfull.argus.utils :refer [ident]]))

(defn find-encoder
  [cache c]
  (when-let [encoder (get @cache c)]
    (swap! cache assoc c encoder)
    encoder))

(def default-encoders
  {cljs.core/PersistentHashSet (fn [o] {"#set" (vec o)})
   cljs.core/PersistentTreeSet (fn [o] {"#set" (vec o)})
   cljs.core/UUID (fn [o] {"#uuid" (str o)})
   ;; java.time.LocalDate (fn [o] {"#date" (str o)})
   ;; java.time.Instant (fn [o] {"#instant" (str o)})
   ;; java.util.Date (fn [o] (java.util.Date/.toInstant o))
   ;; java.sql.Date (fn [o] (java.sql.Date/.toLocalDate o))
   ;; java.sql.Timestamp (fn [o] (java.sql.Timestamp/.toInstant o))
   cljs.core/Keyword (fn [o] {"#clojure/keyword" (ident o)})
   cljs.core/Symbol (fn [o] {"#clojure/symbol" (ident o)})})

(def default-decoders
  {"#set" set
   ;; "#date" java.time.LocalDate/parse
   ;; "#instant" java.time.Instant/parse
   "#uuid" parse-uuid
   "#clojure/keyword" keyword
   "#clojure/symbol" symbol})
