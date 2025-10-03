(ns systems.thoughtfull.argus-test
  (:require
    [clojure.test :refer [deftest is]]
    [systems.thoughtfull.argus :refer [argus deargus enargus]]))

(deftest encode-tagged-value
  (is (= {"#uuid" "62047d3a-112c-4458-900f-606f7fb66a20"}
        (enargus (argus) #uuid "62047d3a-112c-4458-900f-606f7fb66a20"))))

(deftest decode-tagged-value
  (is (= #uuid "62047d3a-112c-4458-900f-606f7fb66a20"
        (deargus (argus) {"#uuid" "62047d3a-112c-4458-900f-606f7fb66a20"}))))

(defrecord CustomType [a b])

(deftest encode-custom-type
  (is (= {"#my/type" [1 2]}
        (enargus (argus :encoders {CustomType ["#my/type" (juxt :a :b)]}) (->CustomType 1 2)))))

(deftest decode-custom-type
  (is (= (->CustomType 1 2)
        (deargus (argus :decoders {"#my/type" (partial apply ->CustomType)}) {"#my/type" [1 2]}))))

(deftest encode-keyword-properties
  (is (= {"foo" "bar"} (enargus (argus) {"foo" "bar"})))
  (is (= {":foo" "bar"} (enargus (argus) {:foo "bar"})))
  (is (= {":foo/baz" "bar"} (enargus (argus) {:foo/baz "bar"})))
  (is (= {"::foo" "bar"} (enargus (argus) {":foo" "bar"})))
  (is (= {":::foo" "bar"} (enargus (argus) {"::foo" "bar"}))))

(deftest decode-keyword-properties
  (is (= {"foo" "bar"} (deargus (argus) {"foo" "bar"})))
  (is (= {:foo "bar"} (deargus (argus) {":foo" "bar"})))
  (is (= {:foo/baz "bar"} (deargus (argus) {":foo/baz" "bar"})))
  (is (= {":foo" "bar"} (deargus (argus) {"::foo" "bar"})))
  (is (= {"::foo" "bar"} (deargus (argus) {":::foo" "bar"}))))

(deftest recursive-coding
  (let [a (argus :encoders {CustomType ["#my/type" #(into {} %)]}
            :decoders {"#my/type" map->CustomType})
        u #uuid "111c2350-fbb5-4988-824b-f15506e896b1"]
    (is (= {"#my/type" {":a" {":c" 1 "::d" 2} ":b"
                        {"#uuid" "111c2350-fbb5-4988-824b-f15506e896b1"}}}
          (enargus a (->CustomType {:c 1 ":d" 2} u))))
    (is (= (->CustomType {:c 1 ":d" 2} u)
          (deargus a {"#my/type" {":a" {":c" 1 "::d" 2} ":b"
                        {"#uuid" "111c2350-fbb5-4988-824b-f15506e896b1"}}})))))
