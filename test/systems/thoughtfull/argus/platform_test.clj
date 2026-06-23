(ns systems.thoughtfull.argus.platform-test
  (:require
    [clojure.test :refer [are deftest is]]
    [systems.thoughtfull.argus :refer [argus deargus enargus]])
  (:import
    (java.time LocalDate)))

(deftest encode-default-types
  (let [a (argus)]
    (are [e o] (= e (enargus a o))
      {"#date" "2025-07-09"}
      (java.time.LocalDate/of 2025 7 9)
      {"#instant" "1970-01-01T00:00:00.987Z"}
      (java.time.Instant/ofEpochMilli 987))))

(deftest decode-default-types
  (let [a (argus)]
    (are [o e] (= o (deargus a e))
      (java.time.LocalDate/of 2025 7 9)
      {"#date" "2025-07-09"}
      (java.time.Instant/ofEpochMilli 987)
      {"#instant" "1970-01-01T00:00:00.987Z"})))

(deftest encode-clojure-types
  (let [a (argus)]
    (are [e o] (= e (enargus a o))
      {"#clojure.bigint" "9223372036854775808"} 9223372036854775808N
      {"#clojure.biginteger" "42"} (biginteger 42)
      {"#clojure.bigdec" "9223372036854775808"} 9223372036854775808M
      {"#clojure.queue" [1 2]} (into clojure.lang.PersistentQueue/EMPTY [1 2])
      {"#clojure.ratio" [{"#clojure.biginteger" "2"} {"#clojure.biginteger" "3"}]} 2/3)))

(deftest decode-clojure-types
  (let [a (argus)]
    (are [o e] (= o (deargus a e))
      9223372036854775808N {"#clojure.bigint" "9223372036854775808"}
      (biginteger 42) {"#clojure.biginteger" "42"}
      9223372036854775808M {"#clojure.bigdec" "9223372036854775808"}
      (into clojure.lang.PersistentQueue/EMPTY [1 2]) {"#clojure.queue" [1 2]}
      2/3 {"#clojure.ratio" [{"#clojure.biginteger" "2"} {"#clojure.biginteger" "3"}]})))

(defrecord CustomType [a b])

(deftest encode-to-builtin-type
  (is (= {"#date" "2025-12-01"}
        (enargus (argus :encoders {CustomType (fn [_] (LocalDate/of 2025 12 1))})
          (map->CustomType {})))))
