(ns ^:no-doc systems.thoughtfull.argus.platform
  (:require
    [systems.thoughtfull.argus.utils :refer [ident]])
  (:import
    (goog.date Date)
    (goog.math Long)))

(defn find-encoder
  [encoders c]
  (get encoders c))

(def default-encoders
  {cljs.core/PersistentHashSet (fn [o] {"#set" (vec o)})
   cljs.core/PersistentTreeSet (fn [o] {"#set" (vec o)})
   cljs.core/UUID (fn [o] {"#uuid" (str o)})
   Date (fn [^Date o] {"#date" (.toIsoString o true)})
   js/Date (fn [o] {"#instant" (.toISOString o)})
   cljs.core/Keyword (fn [o] {"#clojure.keyword" (ident o)})
   cljs.core/List (fn [o] {"#clojure.list" (vec o)})
   cljs.core/PersistentQueue (fn [o] {"#clojure.queue" (vec o)})
   cljs.core/Symbol (fn [o] {"#clojure.symbol" (ident o)})})

(def default-decoders
  {"#set" set
   "#date" #(Date/fromIsoString %)
   "#instant" #(js/Date. %)
   "#integer" #(if (Long/isStringInRange %)
                 (Long/fromString %)
                 {"#integer" %})
   "#uuid" parse-uuid
   "#clojure.keyword" keyword
   "#clojure.list" (partial apply list)
   "#clojure.map" (partial into {})
   "#clojure.queue" (partial into cljs.core/PersistentQueue.EMPTY)
   "#clojure.symbol" symbol})
