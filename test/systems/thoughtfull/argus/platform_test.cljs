(ns systems.thoughtfull.argus.platform-test
  (:require
    [clojure.test :refer [are deftest is]]
    [systems.thoughtfull.argus :refer [argus deargus enargus]])
  (:import
    (goog.date Date)))

(deftest default-types
  (let [a (argus)]
    (are [x] (= x (deargus a (enargus a x)))
      (js/Date. 0)
      #uuid "2fd4edfd-50e5-45a1-90c7-b10b95fa2daa"
      #{1 2 3})))

(deftest clojure-types
  (let [a (argus)]
    (are [x] (= x (deargus a (enargus a x)))
      :foo/bar
      'foo/bar)))

(deftest google-date
  (let [a (argus)
        x (Date. 2025 6 15)]
    (is (.equals x (deargus a (enargus a x))))))
