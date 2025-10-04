(ns ^:no-doc systems.thoughtfull.argus.platform
  (:require
    [systems.thoughtfull.argus.utils :refer [ident]])
  (:import
    (goog.date Date)))

(defn find-encoder
  [cache c]
  (when-let [encoder (get @cache c)]
    (swap! cache assoc c encoder)
    encoder))

(def default-encoders
  {cljs.core/PersistentHashSet (fn [o] {"#set" (vec o)})
   cljs.core/PersistentTreeSet (fn [o] {"#set" (vec o)})
   cljs.core/UUID (fn [o] {"#uuid" (str o)})
   Date (fn [^Date o] {"#date" (.toIsoString o true)})
   js/Date (fn [o] {"#instant" (.toISOString o)})
   cljs.core/Keyword (fn [o] {"#clojure.keyword" (ident o)})
   cljs.core/Symbol (fn [o] {"#clojure.symbol" (ident o)})})

(def default-decoders
  {"#set" set
   "#date" #(Date/fromIsoString %)
   "#instant" #(js/Date. %)
   "#uuid" parse-uuid
   "#clojure.keyword" keyword
   "#clojure.symbol" symbol})
