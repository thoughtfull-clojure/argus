(ns systems.thoughtfull.argus.platform-test
  (:require
    [clojure.test :refer [are deftest]]
    [systems.thoughtfull.argus :refer [argus deargus enargus]]))

(deftest default-types
  (let [a (argus)]
    (are [x] (= x (deargus a (enargus a x)))
      #uuid "2fd4edfd-50e5-45a1-90c7-b10b95fa2daa"
      #{1 2 3})))

(deftest clojure-types
  (let [a (argus)]
    (are [x] (= x (deargus a (enargus a x)))
      :foo/bar
      'foo/bar
      9223372036854775808M
      9223372036854775808N)))
