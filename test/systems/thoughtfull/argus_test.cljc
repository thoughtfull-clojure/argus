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
