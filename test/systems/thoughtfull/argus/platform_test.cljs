(ns systems.thoughtfull.argus.platform-test
  (:require
    [clojure.test :refer [are deftest is]]
    [systems.thoughtfull.argus :refer [argus deargus enargus]])
  (:import
    (goog.date Date)))

(deftest encode-default-types
  (let [a (argus)]
    (are [e o] (= e (enargus a o))
      {"#date" "2025-07-09"}
      (Date/fromIsoString "2025-07-09")
      {"#instant" "1970-01-01T00:00:00.987Z"}
      (js/Date. 987))))

(deftest decode-default-types
  (let [a (argus)]
    (are [o e] (= o (deargus a e))
      (js/Date. 987)
      {"#instant" "1970-01-01T00:00:00.987Z"})))

;; I guess equality is hard?
(deftest decode-goog-date
  (is (.equals (Date/fromIsoString "2025-07-09")
        (deargus (argus) {"#date" "2025-07-09"}))))

(deftest encode-clojure-types
  (let [a (argus)]
    (are [e o] (= e (enargus a o))
      {"#clojure.queue" [1 2]} (into cljs.core/PersistentQueue.EMPTY [1 2]))))

(deftest decode-clojure-types
  (let [a (argus)]
    (are [o e] (= o (deargus a e))
      (into cljs.core/PersistentQueue.EMPTY [1 2]) {"#clojure.queue" [1 2]})))

(defrecord CustomType [a b])

(deftest encode-to-builtin-type
  (is (= {"#date" "2025-07-09"}
        (enargus (argus :encoders {CustomType (fn [_] (Date/fromIsoString "2025-07-09"))})
          (map->CustomType {})))))
