(ns systems.thoughtfull.argus.platform-test
  (:require
    [clojure.test :refer [are deftest is]]
    [systems.thoughtfull.argus :refer [argus deargus enargus]])
  (:import
    (goog.date Date)))

(deftest encode-default-types
  (let [a (argus)]
    (are [e o] (= e (enargus a o))
      {"#set" [1]}
      #{1}
      {"#date" "2025-07-09"}
      (Date/fromIsoString "2025-07-09")
      {"#instant" "1970-01-01T00:00:00.987Z"}
      (js/Date. 987)
      {"#uuid" "2fd4edfd-50e5-45a1-90c7-b10b95fa2daa"}
      #uuid "2fd4edfd-50e5-45a1-90c7-b10b95fa2daa")))

(deftest decode-default-types
  (let [a (argus)]
    (are [o e] (= o (deargus a e))
      #{1}
      {"#set" [1]}
      (js/Date. 987)
      {"#instant" "1970-01-01T00:00:00.987Z"}
      #uuid "2fd4edfd-50e5-45a1-90c7-b10b95fa2daa"
      {"#uuid" "2fd4edfd-50e5-45a1-90c7-b10b95fa2daa"})))

;; I guess equality is hard?
(deftest decode-goog-date
  (is (.equals (Date/fromIsoString "2025-07-09")
        (deargus (argus) {"#date" "2025-07-09"}))))

(deftest encode-clojure-types
  (let [a (argus)]
    (are [e o] (= e (enargus a o))
      {"#clojure.keyword" "foo/bar"} :foo/bar
      {"#clojure.symbol" "foo/bar"} 'foo/bar)))

(deftest decode-clojure-types
  (let [a (argus)]
    (are [o e] (= o (deargus a e))
      :foo/bar {"#clojure.keyword" "foo/bar"}
      'foo/bar {"#clojure.symbol" "foo/bar"})))
