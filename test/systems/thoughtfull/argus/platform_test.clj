(ns systems.thoughtfull.argus.platform-test
  (:require
    [clojure.test :refer [are deftest is]]
    [systems.thoughtfull.argus :refer [argus deargus enargus]]))

(deftest encode-default-types
  (let [a (argus)]
    (are [e o] (= e (enargus a o))
      {"#set" [1]}
      #{1}
      {"#date" "2025-07-09"}
      (java.time.LocalDate/of 2025 7 9)
      {"#instant" "1970-01-01T00:00:00.987Z"}
      (java.time.Instant/ofEpochMilli 987)
      {"#uuid" "2fd4edfd-50e5-45a1-90c7-b10b95fa2daa"}
      #uuid "2fd4edfd-50e5-45a1-90c7-b10b95fa2daa")))

(deftest decode-default-types
  (let [a (argus)]
    (are [o e] (= o (deargus a e))
      #{1}
      {"#set" [1]}
      (java.time.LocalDate/of 2025 7 9)
      {"#date" "2025-07-09"}
      (java.time.Instant/ofEpochMilli 987)
      {"#instant" "1970-01-01T00:00:00.987Z"}
      #uuid "2fd4edfd-50e5-45a1-90c7-b10b95fa2daa"
      {"#uuid" "2fd4edfd-50e5-45a1-90c7-b10b95fa2daa"})))

(deftest encode-clojure-types
  (let [a (argus)]
    (are [e o] (= e (enargus a o))
      {"#clojure.keyword" "foo/bar"} :foo/bar
      {"#clojure.symbol" "foo/bar"} 'foo/bar
      {"#clojure.bigint" "9223372036854775808"} 9223372036854775808N
      {"#clojure.biginteger" "42"} (biginteger 42)
      {"#clojure.bigdec" "9223372036854775808"} 9223372036854775808M
      {"#clojure.ratio" [{"#clojure.biginteger" "2"} {"#clojure.biginteger" "3"}]} 2/3)))

(deftest decode-clojure-types
  (let [a (argus)]
    (are [o e] (= o (deargus a e))
      :foo/bar {"#clojure.keyword" "foo/bar"}
      'foo/bar {"#clojure.symbol" "foo/bar"}
      9223372036854775808N {"#clojure.bigint" "9223372036854775808"}
      (biginteger 42) {"#clojure.biginteger" "42"}
      9223372036854775808M {"#clojure.bigdec" "9223372036854775808"}
      2/3 {"#clojure.ratio" [{"#clojure.biginteger" "2"} {"#clojure.biginteger" "3"}]})))

(deftest custom-encoder-for-builtin-tag
  (is (thrown? Exception #"invalid extension tag"
        (enargus (argus :encoders {clojure.lang.IPersistentSet ["set" str]}) #{1}))))
